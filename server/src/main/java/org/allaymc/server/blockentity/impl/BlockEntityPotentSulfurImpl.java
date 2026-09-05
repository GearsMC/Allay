package org.allaymc.server.blockentity.impl;

import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.interfaces.BlockEntityPotentSulfur;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockEntityPotentSulfurImpl extends BlockEntityImpl implements BlockEntityPotentSulfur {
    public BlockEntityPotentSulfurImpl(BlockEntityInitInfo initInfo,
                                 List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
