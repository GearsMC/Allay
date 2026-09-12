package org.allaymc.server.block.component.container;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.blockentity.interfaces.BlockEntityChest;
import org.allaymc.api.container.Container;
import org.allaymc.server.block.component.BlockBaseComponentImpl;
import org.allaymc.server.component.annotation.Dependency;

/**
 * @author IWareQ
 */
public class BlockChestBaseComponentImpl extends BlockBaseComponentImpl {
    @Dependency
    protected BlockBlockEntityHolderComponent<BlockEntityChest> blockEntityHolderComponent;

    public BlockChestBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);

        var thisChest = blockEntityHolderComponent.getBlockEntity(oldBlock.getPosition());
        if (thisChest == null || thisChest.isPaired()) {
            return;
        }

        var direction = newBlockState.getPropertyValue(BlockPropertyTypes.MINECRAFT_CARDINAL_DIRECTION);
        var blockFace = BlockFace.from(direction);
        for (var face : new BlockFace[]{blockFace.rotateY(), blockFace.rotateYCCW()}) {
            var neighbor = oldBlock.offsetPos(face);
            if (!canPairWith(newBlockState, neighbor.getBlockState())) {
                continue;
            }

            var other = neighbor.getBlockEntity();
            if (other instanceof BlockEntityChest otherChest && !otherChest.isPaired()) {
                if (direction == otherChest.getBlockState().getPropertyValue(BlockPropertyTypes.MINECRAFT_CARDINAL_DIRECTION)) {
                    if (otherChest.tryPairWith(thisChest)) {
                        thisChest.tryPairWith(otherChest);
                        break;
                    }
                }
            }
        }
    }

    protected boolean canPairWith(BlockState blockState, BlockState neighborBlockState) {
        return blockState.getBlockType() == neighborBlockState.getBlockType();
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        var chest = blockEntityHolderComponent.getBlockEntity(block.getPosition());
        if (chest == null) {
            return 0;
        }
        // Use double chest container if paired, otherwise use single container
        Container container = chest.isPaired() ? chest.getDoubleChestContainer() : chest.getContainer();
        return container.calculateComparatorSignal();
    }
}
