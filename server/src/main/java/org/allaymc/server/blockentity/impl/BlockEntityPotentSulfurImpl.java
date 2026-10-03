package org.allaymc.server.blockentity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityPotentSulfurBaseComponent;
import org.allaymc.api.blockentity.interfaces.BlockEntityPotentSulfur;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockEntityPotentSulfurImpl extends BlockEntityImpl implements BlockEntityPotentSulfur {

    @Delegate
    private BlockEntityPotentSulfurBaseComponent potentSulfurBaseComponent;

    public BlockEntityPotentSulfurImpl(BlockEntityInitInfo initInfo,
                                 List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
