package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityAnimalComponent;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityFoxBaseComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityFox;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityFoxImpl extends EntityImpl implements EntityFox {

    @Delegate
    private EntityFoxBaseComponent foxBaseComponent;
    @Delegate
    private EntityLivingComponent livingComponent;
    @Delegate
    private EntityPhysicsComponent physicsComponent;
    @Delegate
    private EntityAIComponent aiComponent;
    @Delegate
    private EntityParallelTickComponent parallelTickComponent;
    @Delegate
    private EntityAnimalComponent animalComponent;
    @Delegate
    private EntityBabyComponent babyComponent;
    @Delegate
    private EntityHeadYawComponent headYawComponent;

    public EntityFoxImpl(EntityInitInfo initInfo,
                         List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
