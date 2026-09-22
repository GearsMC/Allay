package org.allaymc.server.block.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.interfaces.BlockMobSpawnerBehavior;
import org.allaymc.api.blockentity.interfaces.BlockEntityMobSpawner;
import org.allaymc.api.component.Component;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class BlockMobSpawnerBehaviorImpl extends BlockBehaviorImpl implements BlockMobSpawnerBehavior {
    @Delegate
    private BlockBlockEntityHolderComponent<BlockEntityMobSpawner> blockEntityHolderComponent;

    public BlockMobSpawnerBehaviorImpl(
            List<ComponentProvider<? extends Component>> componentProviders) {
        super(componentProviders);
    }
}
