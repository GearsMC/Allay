package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityShulker;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityShulkerImpl extends EntityImpl implements EntityShulker {

    @Delegate
    private EntityLivingComponent livingComponent;
    @Delegate
    private EntityPhysicsComponent physicsComponent;
    @Delegate
    private EntityHeadYawComponent headYawComponent;

    public EntityShulkerImpl(EntityInitInfo initInfo,
                             List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
