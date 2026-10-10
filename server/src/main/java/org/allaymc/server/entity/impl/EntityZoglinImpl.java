package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;
import org.allaymc.api.entity.interfaces.EntityZoglin;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityZoglinImpl extends EntityImpl implements EntityZoglin {

    @Delegate
    private EntityLivingComponent livingComponent;

    @Delegate
    private EntityPhysicsComponent physicsComponent;

    @Delegate
    private EntityAIComponent aiComponent;

    @Delegate
    private EntityParallelTickComponent parallelTickComponent;

    @Delegate
    private EntityHeadYawComponent headYawComponent;

    @Delegate
    private EntityUndeadComponent undeadComponent;

    @Delegate
    private EntityBabyComponent babyComponent;

    public EntityZoglinImpl(EntityInitInfo initInfo, List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
