package org.allaymc.server.entity.component;

import lombok.Getter;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.server.component.annotation.ComponentObject;
import org.allaymc.server.component.annotation.OnInitFinish;
import org.allaymc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.allaymc.server.entity.component.event.CEntityParallelTickEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.joml.Vector3dc;

/**
 * @author daoge_cmd
 */
public class EntityAIComponentImpl implements EntityAIComponent {

    @Identifier.Component
    public static final Identifier IDENTIFIER = new Identifier("minecraft:entity_ai_component");

    @ComponentObject
    protected EntityIntelligent thisEntity;

    @Getter
    protected BehaviorGroupImpl behaviorGroup;

    public EntityAIComponentImpl(BehaviorGroup behaviorGroup) {
        this.behaviorGroup = (BehaviorGroupImpl) behaviorGroup;
    }

    @OnInitFinish
    public void onInitFinish(EntityInitInfo initInfo) {
        behaviorGroup.setEntity(thisEntity);
    }

    @Override
    public void setBehaviorGroup(BehaviorGroup behaviorGroup) {
        this.behaviorGroup = (BehaviorGroupImpl) behaviorGroup;
        this.behaviorGroup.setEntity(thisEntity);
    }

    @Override
    public boolean isManualControlEnabled() {
        return behaviorGroup.isManualControlEnabled();
    }

    @Override
    public void setManualControlEnabled(boolean enabled) {
        behaviorGroup.setManualControlEnabled(enabled);
    }

    @Override
    public void navigateTo(Vector3dc target, float speed) {
        behaviorGroup.navigateTo(target, speed);
    }

    @Override
    public void stopNavigation() {
        behaviorGroup.stopNavigation();
    }

    @Override
    public void lookAt(Vector3dc target) {
        behaviorGroup.lookAt(target);
    }

    @Override
    public void stopLooking() {
        behaviorGroup.stopLooking();
    }

    @EventHandler
    protected void onParallelTick(CEntityParallelTickEvent event) {
        if (!thisEntity.isImmobile() && thisEntity.isAlive()) {
            behaviorGroup.tick();
        }
    }

    @EventHandler
    protected void onTick(CEntityTickEvent event) {
        behaviorGroup.processSyncedActions();
    }
}
