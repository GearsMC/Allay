package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityCushionBaseComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.interfaces.EntityCushion;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityCushionImpl extends EntityImpl implements EntityCushion {

    @Delegate
    private EntityCushionBaseComponent cushionBaseComponent;

    @Delegate
    private EntityLivingComponent livingComponent;

    public EntityCushionImpl(EntityInitInfo initInfo,
                             List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
