package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityCreeperBaseComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.event.entity.EntityExplodeEvent;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.explosion.Explosion;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

/**
 * Creeper attack: walk up to the target, hiss, swell, and detonate.
 *
 * <p>The fuse only burns while the target stays close. Step out of {@code fuseRange} and the
 * creeper calms down and resumes the chase, exactly like vanilla — that back-off is what makes a
 * creeper survivable at all.</p>
 *
 * <p>The explosion is created with block destruction switched off. It still hurts and throws
 * entities; it just leaves the terrain intact, which is what a skyblock island needs.</p>
 */
public class SwellAndExplodeExecutor implements BehaviorExecutor {

    protected final MemoryType<Long> targetIdMemory;
    protected final float speed;
    protected final double maxSenseRangeSquared;
    protected final double fuseRangeSquared;
    protected final boolean clearTargetAfterLose;
    protected final int fuseTime;
    protected final float explosionSize;

    protected int fuseTick;
    protected Vector3d lastTargetPos;

    /**
     * Creates a creeper-style explode executor.
     *
     * @param targetIdMemory the memory entry that stores the target entity runtime id.
     * @param speed the movement speed while chasing the target.
     * @param maxSenseRange the maximum target tracking range in blocks.
     * @param fuseRange the distance within which the fuse burns, in blocks.
     * @param clearTargetAfterLose whether to clear the target memory when the behavior stops.
     * @param fuseTime how many ticks the fuse burns before detonating.
     * @param explosionSize the explosion radius.
     */
    public SwellAndExplodeExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                   double fuseRange, boolean clearTargetAfterLose,
                                   int fuseTime, float explosionSize) {
        this.targetIdMemory = targetIdMemory;
        this.speed = speed;
        this.maxSenseRangeSquared = maxSenseRange * maxSenseRange;
        this.fuseRangeSquared = fuseRange * fuseRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
        this.fuseTime = fuseTime;
        this.explosionSize = explosionSize;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        fuseTick = 0;
        lastTargetPos = null;
        entity.setMovementSpeed(speed);
        entity.setPitchEnabled(true);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        if (targetId == null) {
            return false;
        }

        var target = entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityLiving) || !isTargetValid(target)) {
            return false;
        }

        var targetLoc = target.getLocation();
        var distanceSquared = entity.getLocation().distanceSquared(targetLoc);
        if (distanceSquared > maxSenseRangeSquared) {
            return false;
        }

        if (!entity.isPitchEnabled()) {
            entity.setPitchEnabled(true);
        }
        if (entity.getMovementSpeed() != speed) {
            entity.setMovementSpeed(speed);
        }

        EntityControlHelper.setLookTarget(entity, new Vector3d(
                targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()
        ));

        if (distanceSquared > fuseRangeSquared) {
            // Target got away: stop swelling and go back to chasing.
            fuseTick = 0;
            setSwelling(entity, false);

            var chaseTarget = new Vector3d(targetLoc.x(), targetLoc.y(), targetLoc.z());
            entity.setMoveTarget(chaseTarget);
            if (lastTargetPos == null || isInDifferentBlock(lastTargetPos, chaseTarget)) {
                entity.getBehaviorGroup().setRouteUpdateRequired(true);
            }
            lastTargetPos = chaseTarget;
            return true;
        }

        // Close enough — stand still and burn the fuse down.
        EntityControlHelper.removeRouteTarget(entity);
        lastTargetPos = null;

        if (fuseTick == 0) {
            entity.getDimension().addSound(entity.getLocation(), SimpleSound.IGNITE);
            setSwelling(entity, true);
        }

        fuseTick++;
        if (fuseTick < fuseTime) {
            return true;
        }

        explode(entity);
        return false;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setPitchEnabled(false);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        lastTargetPos = null;
        fuseTick = 0;
        setSwelling(entity, false);
        if (clearTargetAfterLose) {
            entity.getMemoryStorage().clear(targetIdMemory);
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    protected void explode(EntityIntelligent entity) {
        var explosion = new Explosion(explosionSize);
        // The whole point: hurt whoever is standing there, leave the island standing.
        explosion.setDestroyBlocks(false);
        explosion.setSpawnFire(false);
        explosion.setEntity(entity);

        var event = new EntityExplodeEvent(entity, explosion);
        if (!event.call()) {
            fuseTick = 0;
            setSwelling(entity, false);
            return;
        }

        var location = entity.getLocation();
        entity.remove();
        explosion.explode(entity.getDimension(), location.x(), location.y(), location.z());
    }

    /**
     * Mirrors the fuse onto the entity so the client can play the swelling animation. The component
     * only broadcasts when the value actually flips, so calling this every tick is free.
     */
    protected void setSwelling(EntityIntelligent entity, boolean swelling) {
        if (entity instanceof EntityCreeperBaseComponent creeper) {
            creeper.setSwelling(swelling);
        }
    }

    protected boolean isInDifferentBlock(Vector3d oldTargetPos, Vector3d newTargetPos) {
        return Math.floor(oldTargetPos.x()) != Math.floor(newTargetPos.x()) ||
               Math.floor(oldTargetPos.y()) != Math.floor(newTargetPos.y()) ||
               Math.floor(oldTargetPos.z()) != Math.floor(newTargetPos.z());
    }

    protected boolean isTargetValid(Entity target) {
        if (!target.isAlive()) {
            return false;
        }

        if (target instanceof EntityPlayer player) {
            var gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
        }

        return true;
    }
}
