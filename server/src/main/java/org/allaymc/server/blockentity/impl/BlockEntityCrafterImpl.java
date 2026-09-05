package org.allaymc.server.blockentity.impl;

import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.interfaces.BlockEntityCrafter;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockEntityCrafterImpl extends BlockEntityImpl implements BlockEntityCrafter {
    public BlockEntityCrafterImpl(BlockEntityInitInfo initInfo,
                                 List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
