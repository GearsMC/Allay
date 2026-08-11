package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.interfaces.EntityVindicator;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityVindicatorImpl extends EntityImpl implements EntityVindicator, EntityContainerHolderComponent {

    @Delegate
    private EntityWeaponStanceComponent weaponStanceComponent;
    @Delegate
    private EntityLivingComponent livingComponent;
    @Delegate
    private EntityContainerHolderComponent containerHolderComponent;
    @Delegate
    private EntityPhysicsComponent physicsComponent;
    @Delegate
    private EntityAIComponent aiComponent;
    @Delegate
    private EntityParallelTickComponent parallelTickComponent;
    @Delegate
    private EntityHeadYawComponent headYawComponent;

    public EntityVindicatorImpl(EntityInitInfo initInfo,
                          List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
