package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.interfaces.EntityWitherSkeleton;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityWitherSkeletonImpl extends EntityImpl implements EntityWitherSkeleton {
    @Delegate
    private EntityLivingComponent livingComponent;
    @Delegate
    private EntityPhysicsComponent physicsComponent;
    @Delegate
    private EntityHeadYawComponent headYawComponent;
    @Delegate
    private EntityContainerHolderComponent containerHolderComponent;
    @Delegate
    private EntityWeaponStanceComponent weaponStanceComponent;

    public EntityWitherSkeletonImpl(EntityInitInfo initInfo,
                                    List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
