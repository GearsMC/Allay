package org.allaymc.server.block.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.component.BlockOxidationComponent;
import org.allaymc.api.block.interfaces.BlockCopperChestBehavior;
import org.allaymc.api.blockentity.interfaces.BlockEntityChest;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockCopperChestBehaviorImpl extends BlockBehaviorImpl implements BlockCopperChestBehavior {
    @Delegate
    private BlockBlockEntityHolderComponent<BlockEntityChest> blockEntityHolderComponent;
    @Delegate
    private BlockOxidationComponent oxidationComponent;

    public BlockCopperChestBehaviorImpl(
            List<ComponentProvider<? extends Component>> componentProviders) {
        super(componentProviders);
    }
}
