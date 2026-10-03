package org.allaymc.server.block.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.interfaces.BlockPotentSulfurBehavior;
import org.allaymc.api.blockentity.interfaces.BlockEntityPotentSulfur;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockPotentSulfurBehaviorImpl extends BlockBehaviorImpl implements BlockPotentSulfurBehavior {
    @Delegate
    private BlockBlockEntityHolderComponent<BlockEntityPotentSulfur> blockEntityHolderComponent;

    public BlockPotentSulfurBehaviorImpl(
            List<ComponentProvider<? extends Component>> componentProviders) {
        super(componentProviders);
    }
}
