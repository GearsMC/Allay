package org.allaymc.server.block.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.interfaces.BlockCrafterBehavior;
import org.allaymc.api.blockentity.interfaces.BlockEntityCrafter;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockCrafterBehaviorImpl extends BlockBehaviorImpl implements BlockCrafterBehavior {
    @Delegate
    private BlockBlockEntityHolderComponent<BlockEntityCrafter> blockEntityHolderComponent;

    public BlockCrafterBehaviorImpl(List<ComponentProvider<? extends Component>> componentProviders) {
        super(componentProviders);
    }
}
