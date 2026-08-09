package org.allaymc.server.item;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemHelper;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.data.ToolTier;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side behaviour for spears: the jab, and the charge attack performed while running.
 * <p>
 * A spear is not an ordinary melee weapon. The client does not send an "attack this entity"
 * transaction for it; it reports a jab and leaves hit detection to the server, because a spear
 * reaches further than a normal attack and uses different hitbox rules. Nothing happens without
 * the code here — the swing animation plays and no damage lands.
 * <p>
 * Two attacks exist:
 * <ul>
 *   <li><b>Jab</b> — a tap. Casts a ray from the eye up to {@value #THRUST_RANGE_MAX} blocks and
 *       damages the nearest living entity inside a narrow cone in front of the player.</li>
 *   <li><b>Charge</b> — holding use while sprinting. Once the hold passes the tier's activation
 *       time the spear repeatedly looks for a target ahead, and damage scales with how fast the
 *       player is actually moving towards it.</li>
 * </ul>
 *
 * @author GearsMC
 */
public final class SpearJab {

    /** How far a jab or charge reaches, in blocks. */
    public static final double THRUST_RANGE_MAX = 4.5d;

    /** How much a target's hitbox grows for the purpose of a spear hit. */
    private static final double TARGET_HITBOX_INFLATION = 0.125d;

    /** The narrowest angle a jab accepts, as a cosine. 0.866 is 30 degrees. */
    private static final double JAB_MIN_FACING_DOT = 0.866d;

    /** The charge attack is more forgiving about aim than a jab. */
    private static final double CHARGE_MIN_FACING_DOT = 0.75d;

    /** Durability spent per landed jab. */
    private static final int DURABILITY_COST = 1;

    /** Extra margin added to the entity search box around the ray. */
    private static final double SEARCH_MARGIN = 1.5d;

    /** Ticks between two charge hits on the same player. */
    private static final int CHARGE_HIT_COOLDOWN_TICKS = 12;

    /** The speed a sprinting player is expected to reach, blocks per tick. */
    private static final double CHARGE_SPRINT_SPEED_REF = 0.28d;

    /** Below this forward speed a charge deals nothing. */
    private static final double CHARGE_MIN_MOVEMENT_SPEED = 0.255d;

    /** How strongly excess speed feeds into charge damage. */
    private static final double CHARGE_SPEED_DAMAGE_SCALE = 2.5d;

    /** Charge damage is this multiple of the spear's attack damage at full speed. */
    private static final double CHARGE_BASE_DAMAGE_FACTOR = 1.5d;

    /** Below this share of the speed window a charge deals nothing. */
    private static final double CHARGE_MIN_RATIO = 0.05d;

    /**
     * Per-player charge bookkeeping. Transient by design: a charge is a momentary
     * state, so nothing here needs to survive a restart.
     */
    private static final Map<Long, ChargeState> CHARGES = new ConcurrentHashMap<>();

    private SpearJab() {
    }

    /**
     * Resolves and applies a jab.
     *
     * @param attacker the player performing the jab
     * @param spear    the spear being used
     * @return {@code true} if the jab landed on a target
     */
    public static boolean performJab(EntityPlayer attacker, ItemStack spear) {
        if (attacker.getGameMode() == GameMode.SPECTATOR || !attacker.isCooldownEnd(spear.getItemType())) {
            return false;
        }
        Dimension dimension = attacker.getDimension();
        if (dimension == null) {
            return false;
        }

        clearCharge(attacker);
        attacker.setCooldown(spear.getItemType(), jabCooldownTicks(spear));

        Vector3d eye = eyeOf(attacker);
        Vector3d direction = directionOf(attacker);
        Entity target = findTarget(attacker, dimension, eye, direction, JAB_MIN_FACING_DOT);

        ToolTier tier = ItemHelper.getToolTier(spear.getItemType());
        if (target == null) {
            dimension.addSound(eye, missSound(tier));
            return false;
        }

        float damage = attackDamageOf(spear);
        boolean hit = ((EntityLivingComponent) target).attack(DamageContainer.entityAttack(attacker, damage));
        if (!hit) {
            dimension.addSound(eye, missSound(tier));
            return false;
        }

        consumeDurability(attacker, spear);
        dimension.addSound(eye, hitSound(tier));
        return true;
    }

    /**
     * Called once per tick while the player holds the spear's use button.
     *
     * @param attacker the charging player
     * @param spear    the spear being held
     * @param usedTime how long the hold has lasted, in ticks
     */
    public static void tickCharge(EntityPlayer attacker, ItemStack spear, long usedTime) {
        ChargeTimings timings = chargeTimings(spear);
        if (usedTime < timings.activation() || usedTime >= timings.total()) {
            return;
        }
        if (attacker.getGameMode() == GameMode.SPECTATOR || !attacker.isCooldownEnd(spear.getItemType())) {
            return;
        }

        ChargeState state = CHARGES.computeIfAbsent(attacker.getRuntimeId(), id -> new ChargeState());
        long now = attacker.getDimension().getWorld().getTick();
        if (now < state.nextHitTick) {
            return;
        }

        // A charge only counts while the player is actually running at the target.
        if (!attacker.isSprinting()) {
            return;
        }

        Dimension dimension = attacker.getDimension();
        Vector3d eye = eyeOf(attacker);
        Vector3d direction = directionOf(attacker);
        Entity target = findTarget(attacker, dimension, eye, direction, CHARGE_MIN_FACING_DOT);
        if (target == null) {
            return;
        }

        double forwardSpeed = forwardSpeedOf(attacker, direction, state);
        double damage = chargeDamage(spear, forwardSpeed);
        if (damage <= 0) {
            return;
        }

        ToolTier tier = ItemHelper.getToolTier(spear.getItemType());
        boolean hit = ((EntityLivingComponent) target).attack(
                DamageContainer.entityAttack(attacker, (float) damage));
        if (!hit) {
            dimension.addSound(eye, missSound(tier));
            return;
        }

        consumeDurability(attacker, spear);
        dimension.addSound(eye, hitSound(tier));
        state.nextHitTick = now + CHARGE_HIT_COOLDOWN_TICKS;
        attacker.setCooldown(spear.getItemType(), CHARGE_HIT_COOLDOWN_TICKS);
    }

    /**
     * Records the player's position so the next tick can measure real movement.
     *
     * <p>The charge needs the speed the player is <em>actually</em> travelling, which the
     * server only learns by sampling position between ticks.</p>
     *
     * @param attacker the charging player
     */
    public static void sampleMovement(EntityPlayer attacker) {
        ChargeState state = CHARGES.get(attacker.getRuntimeId());
        if (state == null) {
            return;
        }
        var location = attacker.getLocation();
        state.sample(location.x(), location.z(), attacker.getDimension().getWorld().getTick());
    }

    /**
     * Forgets a player's charge, on release or on a jab.
     *
     * @param attacker the player
     */
    public static void clearCharge(EntityPlayer attacker) {
        CHARGES.remove(attacker.getRuntimeId());
    }

    /**
     * Plays the sound of a spear being raised.
     *
     * @param player the player raising it
     * @param spear  the spear
     */
    public static void playUseSound(EntityPlayer player, ItemStack spear) {
        Dimension dimension = player.getDimension();
        if (dimension != null) {
            dimension.addSound(eyeOf(player), useSound(ItemHelper.getToolTier(spear.getItemType())));
        }
    }

    // ==================== hit detection ====================

    /**
     * Finds the nearest living entity the ray crosses within reach.
     *
     * @param attacker     the attacking player
     * @param dimension    the dimension to search
     * @param eye          the ray origin
     * @param direction    the normalised look direction
     * @param minFacingDot how tight the aiming cone is
     * @return the target, or {@code null} when nothing is hit
     */
    private static Entity findTarget(EntityPlayer attacker, Dimension dimension,
                                     Vector3d eye, Vector3d direction, double minFacingDot) {
        double searchRadius = THRUST_RANGE_MAX + SEARCH_MARGIN;
        AABBd searchBox = MathUtils.grow(new AABBd(attacker.getOffsetAABB()),
                new Vector3d(searchRadius, searchRadius, searchRadius));

        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity candidate : dimension.getEntityManager().getPhysicsService()
                .computeCollidingEntities(searchBox)) {
            if (candidate == attacker || !(candidate instanceof EntityLivingComponent)
                    || !candidate.isAlive()) {
                continue;
            }
            if (candidate instanceof EntityPlayer && !canAttackPlayers(attacker)) {
                continue;
            }

            AABBd hitbox = MathUtils.grow(new AABBd(candidate.getOffsetAABB()), new Vector3d(
                    TARGET_HITBOX_INFLATION, TARGET_HITBOX_INFLATION, TARGET_HITBOX_INFLATION));
            Vector2d intersection = new Vector2d();
            if (!hitbox.intersectsRay(eye.x, eye.y, eye.z,
                    direction.x, direction.y, direction.z, intersection)) {
                continue;
            }

            // x holds the near intersection distance along the unit ray.
            double distance = intersection.x;
            if (distance < 0 || distance > THRUST_RANGE_MAX || distance >= nearestDistance) {
                continue;
            }

            Vector3d toHit = new Vector3d(direction).mul(distance);
            if (toHit.lengthSquared() > 1.0e-10
                    && direction.dot(toHit.normalize()) < minFacingDot) {
                continue;
            }

            nearest = candidate;
            nearestDistance = distance;
        }
        return nearest;
    }

    // ==================== charge maths ====================

    /**
     * How much damage a charge deals at the given forward speed.
     *
     * @param spear        the spear used
     * @param forwardSpeed the player's speed towards the target, blocks per tick
     * @return the damage, or 0 when the player is too slow
     */
    private static double chargeDamage(ItemStack spear, double forwardSpeed) {
        double excess = Math.max(0, forwardSpeed - CHARGE_MIN_MOVEMENT_SPEED);
        double range = CHARGE_SPRINT_SPEED_REF - CHARGE_MIN_MOVEMENT_SPEED;
        if (range <= 1.0e-6) {
            return 0;
        }
        double ratio = Math.min(1.0, excess / range);
        if (ratio < CHARGE_MIN_RATIO) {
            return 0;
        }
        double base = attackDamageOf(spear) * CHARGE_BASE_DAMAGE_FACTOR * ratio;
        double speedBonus = excess * 20.0 * CHARGE_SPEED_DAMAGE_SCALE * ratio;
        return base + speedBonus;
    }

    /**
     * The player's speed along the direction they are looking.
     *
     * @param attacker  the player
     * @param direction the look direction
     * @param state     the player's charge state, holding the previous sample
     * @return blocks per tick, never negative
     */
    private static double forwardSpeedOf(EntityPlayer attacker, Vector3d direction, ChargeState state) {
        Vector3d look = new Vector3d(direction.x, 0, direction.z);
        if (look.lengthSquared() <= 1.0e-10) {
            return 0;
        }
        look.normalize();
        return Math.max(0, state.velocityX * look.x + state.velocityZ * look.z);
    }

    // ==================== per-tier values ====================

    /**
     * The cooldown a jab imposes, which grows with the spear's tier.
     *
     * @param spear the spear used
     * @return the cooldown in ticks
     */
    private static int jabCooldownTicks(ItemStack spear) {
        ToolTier tier = ItemHelper.getToolTier(spear.getItemType());
        if (tier == null) {
            return 19;
        }
        return switch (tier) {
            case WOODEN -> 13;
            case STONE -> 15;
            case GOLD, IRON -> 19;
            case DIAMOND -> 21;
            case NETHERITE -> 23;
        };
    }

    /**
     * How long a charge takes to activate and how long it stays usable.
     *
     * @param spear the spear used
     * @return the timings for the spear's tier
     */
    private static ChargeTimings chargeTimings(ItemStack spear) {
        ToolTier tier = ItemHelper.getToolTier(spear.getItemType());
        if (tier == null) {
            return new ChargeTimings(13, 34, 85, 90);
        }
        return switch (tier) {
            case WOODEN -> new ChargeTimings(15, 40, 100, 100);
            case GOLD -> new ChargeTimings(14, 40, 100, 105);
            case STONE -> new ChargeTimings(14, 36, 90, 95);
            case IRON -> new ChargeTimings(13, 34, 85, 90);
            case DIAMOND -> new ChargeTimings(12, 30, 70, 70);
            case NETHERITE -> new ChargeTimings(8, 28, 60, 65);
        };
    }

    private static SimpleSound hitSound(ToolTier tier) {
        if (tier == null) {
            return SimpleSound.IRON_SPEAR_ATTACK_HIT;
        }
        return switch (tier) {
            case WOODEN -> SimpleSound.WOODEN_SPEAR_ATTACK_HIT;
            case STONE -> SimpleSound.STONE_SPEAR_ATTACK_HIT;
            case GOLD -> SimpleSound.GOLDEN_SPEAR_ATTACK_HIT;
            case IRON -> SimpleSound.IRON_SPEAR_ATTACK_HIT;
            case DIAMOND -> SimpleSound.DIAMOND_SPEAR_ATTACK_HIT;
            case NETHERITE -> SimpleSound.NETHERITE_SPEAR_ATTACK_HIT;
        };
    }

    private static SimpleSound missSound(ToolTier tier) {
        if (tier == null) {
            return SimpleSound.IRON_SPEAR_ATTACK_MISS;
        }
        return switch (tier) {
            case WOODEN -> SimpleSound.WOODEN_SPEAR_ATTACK_MISS;
            case STONE -> SimpleSound.STONE_SPEAR_ATTACK_MISS;
            case GOLD -> SimpleSound.GOLDEN_SPEAR_ATTACK_MISS;
            case IRON -> SimpleSound.IRON_SPEAR_ATTACK_MISS;
            case DIAMOND -> SimpleSound.DIAMOND_SPEAR_ATTACK_MISS;
            case NETHERITE -> SimpleSound.NETHERITE_SPEAR_ATTACK_MISS;
        };
    }

    private static SimpleSound useSound(ToolTier tier) {
        if (tier == null) {
            return SimpleSound.IRON_SPEAR_USE;
        }
        return switch (tier) {
            case WOODEN -> SimpleSound.WOODEN_SPEAR_USE;
            case STONE -> SimpleSound.STONE_SPEAR_USE;
            case GOLD -> SimpleSound.GOLDEN_SPEAR_USE;
            case IRON -> SimpleSound.IRON_SPEAR_USE;
            case DIAMOND -> SimpleSound.DIAMOND_SPEAR_USE;
            case NETHERITE -> SimpleSound.NETHERITE_SPEAR_USE;
        };
    }

    // ==================== small helpers ====================

    private static float attackDamageOf(ItemStack spear) {
        float damage = spear.getItemType().getItemData().attackDamage();
        return damage <= 0 ? 1 : damage;
    }

    private static Vector3d eyeOf(EntityPlayer player) {
        var location = player.getLocation();
        return new Vector3d(location.x(), location.y() + player.getEyeHeight(), location.z());
    }

    private static Vector3d directionOf(EntityPlayer player) {
        var location = player.getLocation();
        return MathUtils.getDirectionVector(location.yaw(), location.pitch()).normalize();
    }

    private static boolean canAttackPlayers(EntityPlayer attacker) {
        var controller = attacker.getController();
        return controller == null || controller.canAttackPlayers();
    }

    private static void consumeDurability(EntityPlayer attacker, ItemStack spear) {
        spear.tryIncreaseDamage(DURABILITY_COST);
        var inventory = attacker.getContainer(ContainerTypes.INVENTORY);
        if (inventory != null) {
            inventory.setItemStack(inventory.getHandSlot(), spear);
        }
    }

    /**
     * How long a charge takes to activate and how long each stage lasts, in ticks.
     *
     * @param activation  ticks of holding before the charge arms
     * @param engaged     ticks the charge stays at full readiness
     * @param tired       ticks after that, still usable
     * @param disengaged  the final stretch before the charge lapses
     */
    private record ChargeTimings(int activation, int engaged, int tired, int disengaged) {

        /** @return the tick at which the charge lapses entirely */
        int total() {
            return activation + engaged + tired + disengaged;
        }
    }

    /** A player's live charge: movement sampling and the next allowed hit. */
    private static final class ChargeState {

        private double lastX;
        private double lastZ;
        private long lastTick;
        private boolean seeded;

        private double velocityX;
        private double velocityZ;

        private long nextHitTick;

        /**
         * Folds a new position sample into the measured velocity.
         *
         * @param x    current x
         * @param z    current z
         * @param tick current tick
         */
        void sample(double x, double z, long tick) {
            if (seeded) {
                long delta = Math.max(1, Math.min(tick - lastTick, 3));
                velocityX = (x - lastX) / delta;
                velocityZ = (z - lastZ) / delta;
            }
            lastX = x;
            lastZ = z;
            lastTick = tick;
            seeded = true;
        }
    }
}
