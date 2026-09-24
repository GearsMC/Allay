package org.allaymc.server.item.component.weapon;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.eventbus.event.entity.ProjectileLaunchEvent;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.item.enchantment.EnchantmentTypes;
import org.allaymc.api.item.interfaces.ItemTridentStack;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.api.world.sound.TridentRiptideSound;
import org.allaymc.server.item.component.ItemBaseComponentImpl;
import org.joml.Vector3d;

/**
 * Item component implementation for tridents.
 * Handles throwing and riptide mechanics.
 *
 * @author daoge_cmd
 */
public class ItemTridentBaseComponentImpl extends ItemBaseComponentImpl {

    /**
     * Minimum ticks required to charge before throwing.
     */
    protected static final int MIN_CHARGE_TICKS = 5;

    /**
     * En dusuk firlatma hizi — PHP {@code MIN_FORCE}.
     */
    protected static final double MIN_FORCE = 1.15;

    /**
     * En yuksek firlatma hizi — PHP {@code MAX_FORCE}.
     */
    protected static final double MAX_FORCE = 3.6;

    /**
     * Eklenti dunya/hava kurali kapisi: girdap burada kullanilamazsa
     * mizrak firlatilir (PHP {@code canActivateRiptide} basarisizligi).
     * Varsayilan serbest; GearsCore kurulumda baglar.
     */
    public static java.util.function.Predicate<EntityPlayer> riptideGuard = player -> true;

    /**
     * Girdap itki tabani (saniye basina blok) — PHP {@code 3.0}.
     */
    protected static final double RIPTIDE_FORCE_BASE = 3.0;

    /**
     * Girdap itki adimi (tik) — PHP {@code RIPTIDE_MOTION_TICKS}.
     */
    protected static final int RIPTIDE_MOTION_TICKS = 10;

    /**
     * Girdap donusu suresi (tik) — PHP {@code 10 + seviye * 5}.
     */
    protected static int riptideSpinDuration(int level) {
        return 10 + level * 5;
    }

    public ItemTridentBaseComponentImpl(ItemStackInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public boolean canUseItemInAir(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean useItemInAir(EntityPlayer player, long usedTime) {
        if (usedTime < MIN_CHARGE_TICKS) {
            return false;
        }

        // Cannot throw trident with 1 or less durability remaining
        // See: https://minecraft.wiki/w/Trident
        if (getMaxDamage() - getDamage() <= 1) {
            return false;
        }

        // Calculate force based on charge time (PHP ile ayni egri)
        double p = usedTime / 20.0;
        double force = Math.min((p * p + p * 2) / 3.0, 1.0);

        // PHP MIN_FORCE esigi hiz cinsindendir (1.15 blok/tik).
        if (force * MAX_FORCE < MIN_FORCE) {
            return false;
        }

        // Check for riptide enchantment (PHP: hava sarti tutmuyorsa firlatilir)
        int riptideLevel = getEnchantmentLevel(EnchantmentTypes.RIPTIDE);
        if (riptideLevel > 0 && riptideGuard.test(player)) {
            return handleRiptide(player, riptideLevel);
        }

        return throwTrident(player, force);
    }

    /**
     * Throws the trident as a projectile.
     */
    protected boolean throwTrident(EntityPlayer player, double force) {
        var dimension = player.getDimension();
        var location = player.getLocation();
        var shootPos = new Vector3d(location.x(), location.y() + player.getEyeHeight() - 0.1, location.z());

        // Calculate motion based on player's look direction (PHP MAX_FORCE = 3.6)
        var direction = MathUtils.getDirectionVector(location);
        var speed = force * MAX_FORCE;
        var motion = direction.mul(speed);

        // Add player's motion if on ground
        var playerMotion = new Vector3d(player.getMotion());
        if (player.isOnGround()) {
            playerMotion.y = 0;
        }
        motion.add(playerMotion);

        // Create trident entity
        var trident = EntityTypes.THROWN_TRIDENT.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(shootPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(motion)
                        .build()
        );

        if (trident == null) {
            return false;
        }

        // Set up trident properties
        trident.setShooter(player);

        // Store the trident item for later return (loyalty) or drop
        var tridentItem = (ItemTridentStack) thisItemStack.copy();
        tridentItem.setCount(1);
        trident.setTridentItem(tridentItem);

        // Set favored slot for loyalty return
        int loyaltyLevel = getEnchantmentLevel(EnchantmentTypes.LOYALTY);
        if (loyaltyLevel > 0) {
            trident.setFavoredSlot(player.getContainer(ContainerTypes.INVENTORY).getHandSlot());
        }

        // Fire event
        var event = new ProjectileLaunchEvent(trident, player, speed);
        if (!event.call()) {
            return false;
        }
        // Eklenti kuvveti degistirdiyse (ör. dunya kurallari) uygula.
        if (event.getThrowForce() != speed) {
            trident.setMotion(MathUtils.getDirectionVector(location)
                    .mul(event.getThrowForce(), new Vector3d()));
        }

        // Spawn trident
        dimension.getEntityManager().addEntity(trident);

        // Play sound
        dimension.addSound(shootPos, SimpleSound.TRIDENT_THROW);

        // Reduce durability in survival mode
        if (player.getGameMode() != GameMode.CREATIVE) {
            tryIncreaseDamage(1);
        }

        // Remove from hand (trident is now thrown)
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.clearItemInHand();
        }

        return true;
    }

    /**
     * Handles riptide mechanic - propels the player instead of throwing the trident.
     * Only works when the player is in water or rain.
     * Level I = 9 blocks, Level II = 15 blocks, Level III = 21 blocks
     */
    protected boolean handleRiptide(EntityPlayer player, int riptideLevel) {
        // Check if player is in water or rain
        if (!player.canUseRiptide()) {
            return false;
        }

        // Cannot use riptide with 1 or less durability remaining
        if (player.getGameMode() != GameMode.CREATIVE && getMaxDamage() - getDamage() <= 1) {
            return false;
        }

        var dimension = player.getDimension();
        var location = player.getLocation();

        // PHP: itki = 3.0 * (1 + seviye) / 4.0; hareket 10 tik boyunca
        // yeniden uygulanir ve donus suresi 10 + seviye * 5 tiktir.
        var direction = MathUtils.getDirectionVector(location);
        var speed = RIPTIDE_FORCE_BASE * (1 + riptideLevel) / 4.0;

        // Apply propulsion to player
        var propulsion = direction.mul(speed, new Vector3d());
        player.setMotion(propulsion);

        // Set spin attack state
        player.setSpinAttacking(true);
        player.resetFallDistance();

        var spinDuration = riptideSpinDuration(riptideLevel);
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        dimension.getScheduler().scheduleRepeating(player, () -> {
            if (counter.incrementAndGet() >= RIPTIDE_MOTION_TICKS || !player.isAlive()) {
                player.setSpinAttacking(false);
                return false;
            }
            player.setMotion(direction.mul(speed, new Vector3d()));
            return true;
        }, 1);
        dimension.getScheduler().scheduleDelayed(player,
                () -> player.setSpinAttacking(false), spinDuration);

        // Play riptide sound
        dimension.addSound(location, new TridentRiptideSound(riptideLevel));

        // Reduce durability in survival mode
        if (player.getGameMode() != GameMode.CREATIVE) {
            tryIncreaseDamage(1);
        }

        return true;
    }
}
