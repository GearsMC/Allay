package org.allaymc.server.block.component.liquid;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.eventbus.event.block.LiquidHardenEvent;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.dimension.DimensionType;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3ic;

import static org.allaymc.api.block.component.BlockLiquidBaseComponent.canWaterExistAt;
import static org.allaymc.api.block.component.BlockLiquidBaseComponent.isSource;
import static org.allaymc.api.block.type.BlockTypes.AIR;

/**
 * @author daoge_cmd
 */
public class BlockWaterBaseComponentImpl extends BlockLiquidBaseComponentImpl {
    public BlockWaterBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean isSameLiquidType(BlockType<?> blockType) {
        return blockType.hasBlockTag(BlockTags.WATER);
    }

    /** PM {@code WaterHelper::tryEvaporateUnsupportedWater}: desteksiz su buharlasir. */
    private static boolean tryEvaporate(Block block) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        if (canWaterExistAt(dimension, pos)) {
            return false;
        }
        dimension.setLiquid(pos, null);
        var center = MathUtils.center(pos);
        dimension.addSound(center, SimpleSound.FIZZ);
        dimension.addParticle(center.x(), center.y(), center.z(), SimpleParticle.SMOKE);
        return true;
    }

    @Override
    protected boolean canFormSourceAt(Dimension dimension, Vector3ic pos) {
        return !dimension.getDimensionType().waterEvaporates();
    }

    @Override
    protected boolean flowInto(Dimension dimension, Vector3ic src, int srcLayer, BlockState liquid,
                               Vector3ic pos, boolean falling) {
        if (!canWaterExistAt(dimension, pos)) {
            return false;
        }
        return super.flowInto(dimension, src, srcLayer, liquid, pos, falling);
    }

    @Override
    public void onScheduledUpdate(Block block) {
        if (tryEvaporate(block)) {
            return;
        }
        super.onScheduledUpdate(block);
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        if (tryEvaporate(block)) {
            return;
        }

        super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        if (tryEvaporate(new Block(newBlockState, oldBlock.getPosition(), oldBlock.getLayer()))) {
            return;
        }

        super.afterPlaced(oldBlock, newBlockState, placementInfo);
    }

    @Override
    public void onReplace(Block block, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.onReplace(block, newBlockState, placementInfo);

        if (!isSource(block.getBlockState())) {
            // Only source block can be moved to layer 1
            return;
        }

        if (block.getLayer() != 0) {
            return;
        }

        var dim = block.getDimension();
        if (newBlockState.getBlockType() != AIR && newBlockState.getBlockStateData().canContainLiquidSource()) {
            // If the old block is water and the new block can contain liquid,
            // we need to move water to layer 1
            dim.setBlockState(block.getPosition(), block.getBlockState(), 1);
        }
    }

    @Override
    public void afterNeighborLayerReplace(Block currentBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterNeighborLayerReplace(currentBlock, newBlockState, placementInfo);

        if (currentBlock.getLayer() != 0 || isSameLiquidType(newBlockState.getBlockType())) {
            return;
        }

        var dim = currentBlock.getDimension();
        var pos = currentBlock.getPosition();
        if (newBlockState.getBlockType() == AIR) {
            if (isSource(dim.getBlockState(pos, 1))) {
                // Move layer 1 water back to layer 0 only when the liquid is a source liquid
                dim.setBlockState(pos, BlockTypes.WATER.getDefaultState(), 0);
            }
            dim.setBlockState(pos, BlockTypes.AIR.getDefaultState(), 1);
            return;
        }

        if (!newBlockState.getBlockStateData().canContainLiquidSource()) {
            // New layer 0 block cannot contain liquid, remove layer 1 water
            dim.setBlockState(pos, BlockTypes.AIR.getDefaultState(), 1);
        }
    }

    @Override
    public void onEntityInside(Block block, Entity entity) {
        if (entity instanceof EntityLiving living && living.isOnFire()) {
            living.extinguish();
            living.getDimension().addParticle(living.getLocation(), SimpleParticle.WHITE_SMOKE);
            living.getDimension().addSound(living.getLocation(), SimpleSound.FIRE_EXTINGUISH);
        }
    }

    @Override
    public boolean tryHarden(Block block, Block flownIntoBy) {
        if (flownIntoBy == null) {
            return false;
        }

        var dimension = block.getDimension();
        // This method also considered BlockTypes.FLOWING_LAVA as the same liquid type
        if (!BlockTypes.LAVA.getBlockBehavior().isSameLiquidType(flownIntoBy.getBlockType())) {
            return false;
        }

        // SkyBuild Lava::flowIntoBlock: lava suya aktiginda her yonde SU blogu tasa doner
        // (vanilla'daki ust/yan cobblestone ayrimi yok).
        Vector3ic hardenedBlockPosition = block.getPosition();
        BlockState hardenedBlockState = BlockTypes.STONE.getDefaultState();
        var event = new LiquidHardenEvent(flownIntoBy, block.getBlockState(), hardenedBlockState, hardenedBlockPosition);
        if (!event.call()) {
            return false;
        }

        dimension.setBlockState(hardenedBlockPosition, event.getHardenedBlockState());
        flownIntoBy.addSound(SimpleSound.FIZZ);
        return true;
    }

    @Override
    public int getFlowDecay(DimensionType dimensionType) {
        return 1;
    }

    @Override
    public int getFlowSpeed(DimensionType dimensionType) {
        return 5;
    }

    @Override
    public boolean canFormSource() {
        return true;
    }
}
