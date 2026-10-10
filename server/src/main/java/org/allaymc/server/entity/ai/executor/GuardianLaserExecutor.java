package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.data.Difficulty;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.mob.EntityGuardianBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.joml.Vector3d;

public class GuardianLaserExecutor implements BehaviorExecutor {

    protected final MemoryType<Long> targetIdMemory;
    protected final double maxRangeSquared;
    protected final boolean clearTargetAfterLose;
    protected final int coolDown;
    protected final int chargeTime;
    protected final float damage;

    protected int coolDownTick;
    protected int chargeTick;

    public GuardianLaserExecutor(MemoryType<Long> targetIdMemory, double maxRange, boolean clearTargetAfterLose,
                                 int coolDown, int chargeTime, float damage) {
        this.targetIdMemory = targetIdMemory;
        this.maxRangeSquared = maxRange * maxRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
        this.coolDown = coolDown;
        this.chargeTime = chargeTime;
        this.damage = damage;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        coolDownTick = coolDown;
        chargeTick = 0;
        entity.setPitchEnabled(true);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        if (targetId == null) {
            return false;
        }

        var target = entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityLiving living) || !isTargetValid(entity, living)) {
            return false;
        }

        var targetLoc = target.getLocation();
        EntityControlHelper.setLookTarget(entity, new Vector3d(targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()));
        if (entity.getLocation().distanceSquared(targetLoc) > maxRangeSquared) {
            stopLaser(entity);
            return chargeTick == 0;
        }

        if (chargeTick == 0) {
            if (++coolDownTick < coolDown) {
                return true;
            }

            chargeTick = 1;
            EntityControlHelper.removeRouteTarget(entity);
            guardian(entity).setLaserTarget(target);
            entity.getDimension().addSound(entity.getLocation(), new CustomSound(SoundNames.MOB_GUARDIAN_ATTACK_LOOP));
            return true;
        }

        if (++chargeTick < chargeTime) {
            return true;
        }

        var difficulty = entity.getWorld().getWorldData().getDifficulty();
        living.attack(DamageContainer.entityAttack(entity, MobMeleeAttackExecutor.scaleForDifficulty(damage, difficulty)));
        if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
            living.attack(DamageContainer.magicEffect(1), true);
        }
        stopLaser(entity);
        coolDownTick = 0;
        return living.isAlive();
    }

    protected boolean isTargetValid(EntityIntelligent entity, EntityLiving target) {
        if (!target.isAlive() || target == entity || !EntityControlHelper.allowsTarget(entity, target)) {
            return false;
        }

        if (target instanceof EntityPlayer player) {
            var gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
        }

        return true;
    }

    protected void stopLaser(EntityIntelligent entity) {
        chargeTick = 0;
        guardian(entity).setLaserTarget(null);
    }

    protected EntityGuardianBaseComponentImpl guardian(EntityIntelligent entity) {
        return (EntityGuardianBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        stopLaser(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setPitchEnabled(false);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        if (clearTargetAfterLose) {
            entity.getMemoryStorage().clear(targetIdMemory);
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }
}
