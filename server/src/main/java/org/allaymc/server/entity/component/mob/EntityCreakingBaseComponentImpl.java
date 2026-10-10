package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.property.enums.CreakingState;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.ai.executor.EntityControlHelper;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

public class EntityCreakingBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected static final double OBSERVE_RANGE = 24;
    protected static final double LOOK_THRESHOLD = 0.8;

    @Dependency
    protected EntityAIComponent aiComponent;

    protected volatile boolean observed;
    protected int swayingTicks;

    public EntityCreakingBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.9, 2.7);
    }

    public boolean isObserved() {
        return observed;
    }

    public void startSwaying() {
        swayingTicks = 6;
        setPropertyValue(EntityPropertyTypes.CREAKING_SWAYING_TICKS, swayingTicks);
        if (getDimension() != null) {
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_CREAKING_SWAY));
        }
    }

    @EventHandler
    protected void onCreakingTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive()) {
            return;
        }

        var memory = aiComponent.getMemoryStorage();
        var hostile = memory.get(MemoryTypes.ATTACK_TARGET) != null || memory.get(MemoryTypes.NEAREST_PLAYER) != null;
        var nowObserved = hostile && isLookedAt();
        if (nowObserved != observed) {
            getDimension().addSound(location, new CustomSound(nowObserved ? SoundNames.MOB_CREAKING_FREEZE : SoundNames.MOB_CREAKING_UNFREEZE));
        }
        observed = nowObserved;

        var state = !hostile ? CreakingState.NEUTRAL : observed ? CreakingState.HOSTILE_OBSERVED : CreakingState.HOSTILE_UNOBSERVED;
        var previous = getPropertyValue(EntityPropertyTypes.CREAKING_STATE);
        if (previous != state) {
            if (previous == CreakingState.NEUTRAL) {
                getDimension().addSound(location, new CustomSound(SoundNames.MOB_CREAKING_ACTIVATE));
            } else if (state == CreakingState.NEUTRAL) {
                getDimension().addSound(location, new CustomSound(SoundNames.MOB_CREAKING_DEACTIVATE));
            }
            setPropertyValue(EntityPropertyTypes.CREAKING_STATE, state);
        }

        if (swayingTicks > 0) {
            swayingTicks--;
            setPropertyValue(EntityPropertyTypes.CREAKING_SWAYING_TICKS, swayingTicks);
        }
    }

    protected boolean isLookedAt() {
        var rangeSquared = OBSERVE_RANGE * OBSERVE_RANGE;
        var aabb = getOffsetAABB();
        var centerX = (aabb.minX() + aabb.maxX()) / 2;
        var centerY = (aabb.minY() + aabb.maxY()) / 2;
        var centerZ = (aabb.minZ() + aabb.maxZ()) / 2;
        for (var controller : getDimension().getPlayers()) {
            var player = controller.getControlledEntity();
            if (player == null || !player.isAlive() || player.getLocation().distanceSquared(location) > rangeSquared) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (EntityControlHelper.isLookingAt(player, centerX, centerY, centerZ, LOOK_THRESHOLD)
                    || EntityControlHelper.isLookingAt(player, centerX, aabb.maxY() - 0.3, centerZ, LOOK_THRESHOLD)) {
                return true;
            }
        }
        return false;
    }
}
