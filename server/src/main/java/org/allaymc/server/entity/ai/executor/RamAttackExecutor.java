package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.mob.EntityGoatBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.joml.Vector3d;

public class RamAttackExecutor implements BehaviorExecutor {

    public static final double MIN_RAM_DISTANCE = 4;
    public static final double MAX_RAM_DISTANCE = 7;

    protected static final int PREPARE_TICKS = 20;
    protected static final int MAX_CHARGE_TICKS = 60;
    protected static final double HIT_DISTANCE_SQUARED = 2.25;
    protected static final double KNOCKBACK = 2.5;
    protected static final double KNOCKBACK_VERTICAL = 0.1;

    protected final MemoryType<Long> targetIdMemory;
    protected final float ramSpeed;

    protected int tick;
    protected Vector3d chargeTarget;

    public RamAttackExecutor(MemoryType<Long> targetIdMemory, float ramSpeed) {
        this.targetIdMemory = targetIdMemory;
        this.ramSpeed = ramSpeed;
    }

    public static boolean canRam(EntityIntelligent entity, MemoryType<Long> targetIdMemory) {
        if (!(((EntityImpl) entity).getBaseComponent() instanceof EntityGoatBaseComponentImpl goat)) {
            return false;
        }
        if (goat.getNextRamTick() < 0) {
            goat.setNextRamTick(entity.getTick() + goat.rollRamCooldown());
            return false;
        }
        if (entity.getTick() < goat.getNextRamTick()) {
            return false;
        }

        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        if (target == null || !target.isAlive()) {
            return false;
        }

        var distanceSquared = entity.getLocation().distanceSquared(target.getLocation());
        return distanceSquared >= MIN_RAM_DISTANCE * MIN_RAM_DISTANCE && distanceSquared <= MAX_RAM_DISTANCE * MAX_RAM_DISTANCE;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tick = 0;
        chargeTarget = null;
        EntityControlHelper.removeRouteTarget(entity);
        var goat = goat(entity);
        goat.setRamming(true);
        entity.getDimension().addSound(entity.getLocation(), new CustomSound(goat.isScreamer()
                ? SoundNames.MOB_GOAT_PREPARE_RAM_SCREAMER
                : SoundNames.MOB_GOAT_PREPARE_RAM));
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityLiving living) || !living.isAlive() || !EntityControlHelper.allowsTarget(entity, target)) {
            return false;
        }

        var targetLoc = target.getLocation();
        tick++;
        if (tick < PREPARE_TICKS) {
            EntityControlHelper.setLookTarget(entity, new Vector3d(targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()));
            return true;
        }

        if (chargeTarget == null) {
            var loc = entity.getLocation();
            var direction = new Vector3d(targetLoc.x() - loc.x(), 0, targetLoc.z() - loc.z());
            if (direction.lengthSquared() < 1e-4) {
                return false;
            }
            direction.normalize(2);
            chargeTarget = new Vector3d(targetLoc.x(), targetLoc.y(), targetLoc.z()).add(direction);
            entity.setMovementSpeed(ramSpeed);
            entity.setMoveTarget(chargeTarget);
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }

        if (entity.getLocation().distanceSquared(targetLoc) <= HIT_DISTANCE_SQUARED) {
            hit(entity, living);
            return false;
        }

        return tick < PREPARE_TICKS + MAX_CHARGE_TICKS && entity.getLocation().distanceSquared(chargeTarget) > 1;
    }

    protected void hit(EntityIntelligent entity, EntityLiving target) {
        var goat = goat(entity);
        var damage = DamageContainer.entityAttack(entity, goat.isBaby() ? 1 : 2);
        damage.setKnockback(KNOCKBACK);
        damage.setKnockbackVertical(KNOCKBACK_VERTICAL);
        target.attack(damage);
        entity.getDimension().addSound(entity.getLocation(), new CustomSound(goat.isScreamer()
                ? SoundNames.MOB_GOAT_RAM_IMPACT_SCREAMER
                : SoundNames.MOB_GOAT_RAM_IMPACT));
    }

    protected EntityGoatBaseComponentImpl goat(EntityIntelligent entity) {
        return (EntityGoatBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        var goat = goat(entity);
        goat.setRamming(false);
        goat.setNextRamTick(entity.getTick() + goat.rollRamCooldown());
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        chargeTarget = null;
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }
}
