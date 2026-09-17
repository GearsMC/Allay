package org.allaymc.server.block.component;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.world.Dimension;
import org.allaymc.server.block.connection.BlockConnectionUpdater;
import org.joml.Vector3ic;

/**
 * Cam panel: camın düşürme kuralı ({@link BlockGlassBaseComponentImpl}) + bağlantı durumları
 * ({@link BlockConnectionBaseComponentImpl} ile aynı çağrılar).
 *
 * <p>GearsMC fork: 26.50 bağlantı durumları.</p>
 */
public class BlockGlassPaneBaseComponentImpl extends BlockGlassBaseComponentImpl {
    public BlockGlassPaneBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean place(Dimension dimension, BlockState blockState, Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        return super.place(dimension, BlockConnectionUpdater.compute(dimension, placeBlockPos, blockState), placeBlockPos, placementInfo);
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);
        BlockConnectionUpdater.refresh(oldBlock.getDimension(), oldBlock.getPosition());
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
        if (face.isHorizontal()) {
            BlockConnectionUpdater.refresh(block.getDimension(), block.getPosition());
        }
    }
}
