package org.allaymc.server.block.component.container;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.interfaces.BlockCopperChestBehavior;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;

public class BlockCopperChestBaseComponentImpl extends BlockChestBaseComponentImpl {

    public BlockCopperChestBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);
        syncOxidationToPair(oldBlock, newBlockState);
    }

    @Override
    protected boolean canPairWith(BlockState blockState, BlockState neighborBlockState) {
        return neighborBlockState.getBehavior() instanceof BlockCopperChestBehavior;
    }

    protected void syncOxidationToPair(Block oldBlock, BlockState newBlockState) {
        if (!(oldBlock.getBlockState().getBehavior() instanceof BlockCopperChestBehavior)) {
            return;
        }

        var thisChest = blockEntityHolderComponent.getBlockEntity(oldBlock.getPosition());
        if (thisChest == null || !thisChest.isPaired()) {
            return;
        }

        var pair = thisChest.getPair();
        if (pair == null) {
            return;
        }

        var pairPos = pair.getPosition();
        var pairBlockState = pairPos.dimension().getBlockState(pairPos);
        if (pairBlockState.getBlockType() == newBlockState.getBlockType() ||
            !(pairBlockState.getBehavior() instanceof BlockCopperChestBehavior)) {
            return;
        }

        pairPos.dimension().setBlockState(pairPos, newBlockState.getBlockType().copyPropertyValuesFrom(pairBlockState));
    }
}
