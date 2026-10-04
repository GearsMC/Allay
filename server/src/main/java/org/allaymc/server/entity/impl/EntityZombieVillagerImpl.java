package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;
import org.allaymc.api.entity.interfaces.EntityZombieVillager;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityZombieVillagerImpl extends EntityImpl implements EntityZombieVillager {

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
    private EntityContainerHolderComponent containerHolderComponent;

    @Delegate
    private EntityBabyComponent babyComponent;

    public EntityZombieVillagerImpl(EntityInitInfo initInfo, List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
