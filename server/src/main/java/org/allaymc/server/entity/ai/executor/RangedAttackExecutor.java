package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.action.SimpleEntityAction;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.data.WeaponStance;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

/**
 * Executes a ranged chase-and-shoot behavior against a target entity stored in memory.
 *
 * <p>Counterpart of {@link MeleeAttackExecutor} for mobs that fight with a bow or crossbow.
 * Instead of closing all the way in, the mob keeps the target inside a distance band: it walks
 * closer when the target is beyond {@code preferredRange} and backs away when the target gets
 * closer than {@code minRange}, so a player cannot simply walk up to it and stand there.</p>
 */
public class RangedAttackExecutor implements BehaviorExecutor {

    /** Ticks the weapon is held fully drawn on target before the shot goes off. */
    protected static final int AIM_TIME = 10;

    protected final MemoryType<Long> targetIdMemory;
    protected final float speed;
    protected final double maxSenseRangeSquared;
    protected final double preferredRangeSquared;
    protected final double minRangeSquared;
    protected final boolean clearTargetAfterLose;
    protected final int coolDown;
    protected final float arrowVelocity;
    protected final float arrowBaseDamage;

    protected int attackTick;
    protected Vector3d lastTargetPos;

    /**
     * Creates a ranged attack executor.
     *
     * @param targetIdMemory the memory entry that stores the target entity runtime id.
     * @param speed the movement speed while repositioning.
     * @param maxSenseRange the maximum target tracking range in blocks.
     * @param preferredRange the distance the mob tries to shoot from, in blocks.
     * @param minRange the distance below which the mob backs away, in blocks.
     * @param clearTargetAfterLose whether to clear the target memory when the behavior stops.
     * @param coolDown the shoot cooldown in ticks.
     */
    public RangedAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                double preferredRange, double minRange,
                                boolean clearTargetAfterLose, int coolDown) {
        this(targetIdMemory, speed, maxSenseRange, preferredRange, minRange, clearTargetAfterLose, coolDown, 2.5f, 2f);
    }

    /**
     * Creates a ranged attack executor with custom projectile values.
     *
     * @param targetIdMemory the memory entry that stores the target entity runtime id.
     * @param speed the movement speed while repositioning.
     * @param maxSenseRange the maximum target tracking range in blocks.
     * @param preferredRange the distance the mob tries to shoot from, in blocks.
     * @param minRange the distance below which the mob backs away, in blocks.
     * @param clearTargetAfterLose whether to clear the target memory when the behavior stops.
     * @param coolDown the shoot cooldown in ticks.
     * @param arrowVelocity the speed the fired arrow is launched with.
     * @param arrowBaseDamage the base damage of the fired arrow.
     */
    public RangedAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                double preferredRange, double minRange,
                                boolean clearTargetAfterLose, int coolDown,
                                float arrowVelocity, float arrowBaseDamage) {
        this.targetIdMemory = targetIdMemory;
        this.speed = speed;
        this.maxSenseRangeSquared = maxSenseRange * maxSenseRange;
        this.preferredRangeSquared = preferredRange * preferredRange;
        this.minRangeSquared = minRange * minRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
        this.coolDown = coolDown;
        this.arrowVelocity = arrowVelocity;
        this.arrowBaseDamage = arrowBaseDamage;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        attackTick = 0;
        lastTargetPos = null;
        entity.setMovementSpeed(speed);
        entity.setPitchEnabled(true);
        setWeaponStance(entity, WeaponStance.CHARGING);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        attackTick++;

        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        if (targetId == null) {
            return false;
        }

        var targetEntity = entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(targetEntity instanceof EntityLiving) || !isTargetValid(targetEntity)) {
            return false;
        }

        var entityLoc = entity.getLocation();
        var targetLoc = targetEntity.getLocation();
        var distanceSquared = entityLoc.distanceSquared(targetLoc);
        if (distanceSquared > maxSenseRangeSquared) {
            return false;
        }

        if (!entity.isPitchEnabled()) {
            entity.setPitchEnabled(true);
        }
        if (entity.getMovementSpeed() != speed) {
            entity.setMovementSpeed(speed);
        }

        // Always face the target, even while standing still or retreating.
        EntityControlHelper.setLookTarget(entity, new Vector3d(
                targetLoc.x(), targetLoc.y() + targetEntity.getEyeHeight(), targetLoc.z()
        ));

        updateMovement(entity, entityLoc.x(), entityLoc.z(), targetLoc.x(), targetLoc.y(), targetLoc.z(), distanceSquared);

        // Charge, hold on target, then loose. The hold phase exists so the client has time to show
        // the fully-drawn pose — firing the instant the charge completes reads as a twitch.
        if (attackTick < coolDown) {
            setWeaponStance(entity, WeaponStance.CHARGING);
        } else if (attackTick < coolDown + AIM_TIME) {
            setWeaponStance(entity, WeaponStance.READY);
        } else {
            shoot(entity, targetEntity);
            attackTick = 0;
            setWeaponStance(entity, WeaponStance.CHARGING);
        }

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setPitchEnabled(false);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        lastTargetPos = null;
        // Lower the weapon; otherwise the mob wanders off still holding a fully drawn bow.
        setWeaponStance(entity, WeaponStance.IDLE);
        if (clearTargetAfterLose) {
            entity.getMemoryStorage().clear(targetIdMemory);
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    /**
     * Walks toward the target when it is too far, away from it when it is too close, and stops
     * moving while the target sits inside the comfortable band between the two.
     */
    protected void updateMovement(EntityIntelligent entity, double entityX, double entityZ,
                                  double targetX, double targetY, double targetZ, double distanceSquared) {
        Vector3d moveTarget;
        if (distanceSquared > preferredRangeSquared) {
            moveTarget = new Vector3d(targetX, targetY, targetZ);
        } else if (distanceSquared < minRangeSquared) {
            // Step directly away from the target, keeping the same distance we would have closed.
            var dx = entityX - targetX;
            var dz = entityZ - targetZ;
            var length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1e-4) {
                // Standing exactly on top of the target: any direction works, pick one.
                dx = 1;
                dz = 0;
                length = 1;
            }
            var retreat = Math.sqrt(preferredRangeSquared);
            moveTarget = new Vector3d(entityX + dx / length * retreat, targetY, entityZ + dz / length * retreat);
        } else {
            // Comfortable distance: hold position and just keep shooting.
            EntityControlHelper.removeRouteTarget(entity);
            lastTargetPos = null;
            return;
        }

        entity.setMoveTarget(moveTarget);
        if (lastTargetPos == null || isInDifferentBlock(lastTargetPos, moveTarget)) {
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }
        lastTargetPos = moveTarget;
    }

    protected void shoot(EntityIntelligent entity, Entity target) {
        var dimension = entity.getDimension();
        var location = entity.getLocation();
        var shootPos = new Vector3d(location.x(), location.y() + entity.getEyeHeight() - 0.1, location.z());

        // Aim at the target's eyes instead of its feet, otherwise every arrow lands short.
        var targetLoc = target.getLocation();
        var direction = new Vector3d(
                targetLoc.x() - shootPos.x(),
                targetLoc.y() + target.getEyeHeight() - shootPos.y(),
                targetLoc.z() - shootPos.z()
        );
        if (direction.lengthSquared() < 1e-6) {
            direction = MathUtils.getDirectionVector(location);
        } else {
            direction.normalize();
        }

        var arrow = EntityTypes.ARROW.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(shootPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(direction.mul(arrowVelocity))
                        .build()
        );
        arrow.setShooter(entity);
        arrow.setBaseDamage(arrowBaseDamage);
        // Mob arrows are not lootable, otherwise a mob farm buries the floor in arrows.
        arrow.setPickUpDisabled(true);
        dimension.getEntityManager().addEntity(arrow);

        entity.applyAction(SimpleEntityAction.SWING_ARM);
        dimension.addSound(shootPos, SimpleSound.CROSSBOW_SHOOT);
    }

    /**
     * Mirrors the weapon phase onto the entity so the client can animate it. The component only
     * broadcasts when the value actually flips, so calling this every tick is free.
     */
    protected void setWeaponStance(EntityIntelligent entity, WeaponStance stance) {
        if (entity instanceof EntityWeaponStanceComponent weaponStance) {
            weaponStance.setWeaponStance(stance);
        }
    }

    protected boolean isInDifferentBlock(Vector3d oldTargetPos, Vector3d newTargetPos) {
        return Math.floor(oldTargetPos.x()) != Math.floor(newTargetPos.x()) ||
               Math.floor(oldTargetPos.y()) != Math.floor(newTargetPos.y()) ||
               Math.floor(oldTargetPos.z()) != Math.floor(newTargetPos.z());
    }

    protected boolean isTargetValid(Entity targetEntity) {
        if (!targetEntity.isAlive()) {
            return false;
        }

        if (targetEntity instanceof EntityPlayer player) {
            var gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
        }

        return true;
    }
}
