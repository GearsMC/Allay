package org.allaymc.server.block.component;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.enums.DripstoneThickness;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.eventbus.event.block.BlockFallEvent;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.world.Dimension;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BlockSulfurSpikeBaseComponentImpl extends BlockBaseComponentImpl {

    protected static final int MAX_LENGTH = 7;
    protected static final int MAX_STALACTITE_HEIGHT = 11;
    protected static final int GROW_DENOMINATOR = 5625;
    protected static final int GROW_NUMERATOR = 64;

    public BlockSulfurSpikeBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean place(Dimension dimension, BlockState blockState, Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        var x = placeBlockPos.x();
        var y = placeBlockPos.y();
        var z = placeBlockPos.z();
        if (isReplaceable(dimension.getBlockState(x, y + 1, z)) && isReplaceable(dimension.getBlockState(x, y - 1, z))) {
            return false;
        }

        var tip = blockState.setPropertyValue(BlockPropertyTypes.DRIPSTONE_THICKNESS, DripstoneThickness.TIP);
        return super.place(dimension, tip, placeBlockPos, placementInfo);
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);
        if (placementInfo == null) {
            return;
        }

        var pos = oldBlock.getPosition();
        applyPlacementColumnLogic(oldBlock.getDimension(), pos.x(), pos.y(), pos.z(), placementInfo.blockFace());
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
        var dimension = block.getDimension();
        var pos = block.getPosition();
        if (isHanging(block.getBlockState())) {
            tryFallIfUnsupported(dimension, pos.x(), pos.y(), pos.z());
        } else if (isReplaceable(dimension.getBlockState(pos.x(), pos.y() - 1, pos.z()))) {
            dimension.breakBlock(pos, null, null, false);
        }
    }

    @Override
    public void onBreak(Block block, ItemStack usedItem, Entity entity, List<ItemStack> drops) {
        super.onBreak(block, usedItem, entity, drops);
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var x = pos.x();
        var y = pos.y();
        var z = pos.z();
        var hanging = isHanging(block.getBlockState());
        var thickness = getThickness(block.getBlockState());

        writeBlock(dimension, x, y, z, BlockTypes.AIR.getDefaultState());

        if (thickness == DripstoneThickness.MERGE) {
            var adjacentY = hanging ? y - 1 : y + 1;
            var adjacent = dimension.getBlockState(x, adjacentY, z);
            if (isSpike(adjacent)) {
                writeBlock(dimension, x, adjacentY, z, adjacent.setPropertyValue(BlockPropertyTypes.DRIPSTONE_THICKNESS, DripstoneThickness.TIP));
            }
        }

        if (hanging) {
            rebuildColumnFrom(dimension, x, y + 1, z, BlockFace.UP);
        } else {
            rebuildColumnFrom(dimension, x, y - 1, z, BlockFace.DOWN);
        }
    }

    @Override
    public boolean canRandomUpdate() {
        return true;
    }

    @Override
    public void onRandomUpdate(Block block) {
        super.onRandomUpdate(block);
        if (getThickness(block.getBlockState()) != DripstoneThickness.TIP) {
            return;
        }

        if (ThreadLocalRandom.current().nextInt(1, GROW_DENOMINATOR + 1) <= GROW_NUMERATOR) {
            var pos = block.getPosition();
            tryGrow(block.getDimension(), pos.x(), pos.y(), pos.z());
        }
    }

    protected void applyPlacementColumnLogic(Dimension dimension, int x, int y, int z, BlockFace face) {
        var up = dimension.getBlockState(x, y + 1, z);
        var down = dimension.getBlockState(x, y - 1, z);
        var upIsSpike = isSpike(up);
        var downIsSpike = isSpike(down);

        if (upIsSpike && downIsSpike) {
            setSpike(dimension, x, y, z, false, DripstoneThickness.MERGE);
            setSpike(dimension, x, y + 1, z, true, DripstoneThickness.MERGE);
            return;
        }

        if (upIsSpike) {
            if (!isReplaceable(down)) {
                if (face == BlockFace.UP) {
                    setSpike(dimension, x, y + 1, z, true, DripstoneThickness.MERGE);
                    setSpike(dimension, x, y, z, false, DripstoneThickness.MERGE);
                } else {
                    setSpike(dimension, x, y, z, true, DripstoneThickness.TIP);
                    updateColumnThickness(dimension, x, y, z, true);
                }
                return;
            }
            setSpike(dimension, x, y, z, true, DripstoneThickness.TIP);
            updateColumnThickness(dimension, x, y, z, true);
            return;
        }

        if (!downIsSpike) {
            setSpike(dimension, x, y, z, face != BlockFace.UP, DripstoneThickness.TIP);
            return;
        }

        if (!isReplaceable(up)) {
            if (face == BlockFace.DOWN) {
                setSpike(dimension, x, y, z, true, DripstoneThickness.MERGE);
                setSpike(dimension, x, y - 1, z, false, DripstoneThickness.MERGE);
            } else {
                setSpike(dimension, x, y, z, false, DripstoneThickness.TIP);
                updateColumnThickness(dimension, x, y, z, false);
            }
            return;
        }

        updateColumnThickness(dimension, x, y, z, false);
        setSpike(dimension, x, y, z, false, DripstoneThickness.TIP);
    }

    protected void updateColumnThickness(Dimension dimension, int x, int y, int z, boolean hanging) {
        var length = getColumnLength(dimension, x, y, z, hanging);
        var offset1 = hanging ? 1 : -1;
        var offset2 = hanging ? 2 : -2;

        if (length == 1) {
            setThicknessAt(dimension, x, y + offset1, z, hanging, DripstoneThickness.FRUSTUM);
        } else if (length == 2) {
            setThicknessAt(dimension, x, y + offset2, z, hanging, DripstoneThickness.BASE);
            setThicknessAt(dimension, x, y + offset1, z, hanging, DripstoneThickness.FRUSTUM);
        } else if (length >= 3) {
            setThicknessAt(dimension, x, y + offset2, z, hanging, DripstoneThickness.MIDDLE);
            setThicknessAt(dimension, x, y + offset1, z, hanging, DripstoneThickness.FRUSTUM);
        }
    }

    protected void setThicknessAt(Dimension dimension, int x, int y, int z, boolean hanging, DripstoneThickness thickness) {
        if (isSpike(dimension.getBlockState(x, y, z))) {
            setSpike(dimension, x, y, z, hanging, thickness);
        }
    }

    protected int getColumnLength(Dimension dimension, int x, int y, int z, boolean hanging) {
        if (hanging) {
            for (var j = y + 1; dimension.isYInRange(j); j++) {
                if (!isSpike(dimension.getBlockState(x, j, z))) {
                    return j - y - 1;
                }
            }
        } else {
            for (var j = y - 1; dimension.isYInRange(j); j--) {
                if (!isSpike(dimension.getBlockState(x, j, z))) {
                    return y - j - 1;
                }
            }
        }
        return 0;
    }

    protected void rebuildColumnFrom(Dimension dimension, int x, int y, int z, BlockFace direction) {
        var start = dimension.getBlockState(x, y, z);
        if (!isSpike(start)) {
            return;
        }

        var step = direction == BlockFace.UP ? 1 : -1;
        var column = new ArrayList<Integer>();
        for (var cy = y; isSpike(dimension.getBlockState(x, cy, z)); cy += step) {
            column.add(cy);
        }

        for (var cy : column) {
            writeBlock(dimension, x, cy, z, BlockTypes.AIR.getDefaultState());
        }

        var hanging = isHanging(start);
        for (var i = column.size() - 1; i >= 0; i--) {
            setSpike(dimension, x, column.get(i), z, hanging, DripstoneThickness.TIP);
        }

        if (isSpike(dimension.getBlockState(x, y, z))) {
            updateColumnThickness(dimension, x, y, z, hanging);
        }
    }

    protected int seekColumnEnd(Dimension dimension, int x, int y, int z, BlockFace direction) {
        var step = direction == BlockFace.UP ? 1 : -1;
        var current = y;
        while (isSpike(dimension.getBlockState(x, current + step, z))) {
            current += step;
        }
        return current;
    }

    protected int getColumnHeight(Dimension dimension, int x, int y, int z) {
        return seekColumnEnd(dimension, x, y, z, BlockFace.UP) - seekColumnEnd(dimension, x, y, z, BlockFace.DOWN) + 1;
    }

    protected void tryGrow(Dimension dimension, int x, int y, int z) {
        var hanging = isHanging(dimension.getBlockState(x, y, z));
        var targetY = hanging ? y - 1 : y + 1;
        if (!dimension.isYInRange(targetY)) {
            return;
        }

        if (!isReplaceable(dimension.getBlockState(x, targetY, z))) {
            return;
        }

        if (getColumnHeight(dimension, x, y, z) >= MAX_LENGTH) {
            return;
        }

        if (hanging) {
            if (!canStalactiteGrow(dimension, x, y, z)) {
                return;
            }
        } else if (!canStalagmiteGrow(dimension, x, y, z)) {
            return;
        }

        if (isReplaceable(dimension.getBlockState(x, targetY + 1, z)) && isReplaceable(dimension.getBlockState(x, targetY - 1, z))) {
            return;
        }

        writeBlock(dimension, x, targetY, z, spikeState(false, DripstoneThickness.TIP));
        applyPlacementColumnLogic(dimension, x, targetY, z, hanging ? BlockFace.UP : BlockFace.DOWN);
    }

    protected boolean canStalactiteGrow(Dimension dimension, int x, int y, int z) {
        var topY = seekColumnEnd(dimension, x, y, z, BlockFace.UP);
        if (getColumnHeight(dimension, x, topY, z) >= MAX_LENGTH) {
            return false;
        }

        return dimension.getBlockState(x, topY + 1, z).getBlockType() == BlockTypes.SULFUR;
    }

    protected boolean canStalagmiteGrow(Dimension dimension, int x, int y, int z) {
        if (getColumnHeight(dimension, x, y, z) >= MAX_LENGTH) {
            return false;
        }

        for (var i = 1; i <= MAX_STALACTITE_HEIGHT; i++) {
            var check = dimension.getBlockState(x, y + i, z);
            if (isSpike(check) && isHanging(check) && getThickness(check) == DripstoneThickness.TIP) {
                return true;
            }
            if (!isReplaceable(check) && !isSpike(check)) {
                break;
            }
        }
        return false;
    }

    protected void tryFallIfUnsupported(Dimension dimension, int x, int y, int z) {
        var topY = seekColumnEnd(dimension, x, y, z, BlockFace.UP);
        var support = dimension.getBlockState(x, topY + 1, z).getBlockStateData();
        if (support.isSolid() && support.hasCollision()) {
            return;
        }

        dropHangingColumn(dimension, x, topY, z);
    }

    protected void dropHangingColumn(Dimension dimension, int x, int topY, int z) {
        var column = new ArrayList<BlockState>();
        for (var cy = topY; ; cy--) {
            column.add(dimension.getBlockState(x, cy, z));
            if (!isSpike(dimension.getBlockState(x, cy - 1, z))) {
                break;
            }
        }

        for (var i = 0; i < column.size(); i++) {
            var y = topY - i;
            var state = column.get(i);
            if (!new BlockFallEvent(new Block(state, new Position3i(x, y, z, dimension), 0)).call()) {
                continue;
            }

            writeBlock(dimension, x, y, z, BlockTypes.AIR.getDefaultState());
            var fallingBlock = EntityTypes.FALLING_BLOCK.createEntity(
                    EntityInitInfo.builder()
                            .dimension(dimension)
                            .pos(x + 0.5f, y, z + 0.5f)
                            .build()
            );
            fallingBlock.setBlockState(state);
            dimension.getEntityManager().addEntity(fallingBlock);
        }
    }

    protected static void setSpike(Dimension dimension, int x, int y, int z, boolean hanging, DripstoneThickness thickness) {
        writeBlock(dimension, x, y, z, spikeState(hanging, thickness));
    }

    protected static void writeBlock(Dimension dimension, int x, int y, int z, BlockState state) {
        var pos = new Vector3i(x, y, z);
        var oldState = dimension.getBlockState(pos);
        dimension.setBlockState(pos, state, 0, true, true, false);
        dimension.getBlockUpdateManager().neighborBlockUpdate(pos, pos, BlockFace.UP, oldState);
    }

    protected static BlockState spikeState(boolean hanging, DripstoneThickness thickness) {
        return BlockTypes.SULFUR_SPIKE.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.HANGING, hanging)
                .setPropertyValue(BlockPropertyTypes.DRIPSTONE_THICKNESS, thickness);
    }

    protected static boolean isSpike(BlockState state) {
        return state.getBlockType() == BlockTypes.SULFUR_SPIKE;
    }

    protected static boolean isHanging(BlockState state) {
        return state.getPropertyValue(BlockPropertyTypes.HANGING);
    }

    protected static DripstoneThickness getThickness(BlockState state) {
        return state.getPropertyValue(BlockPropertyTypes.DRIPSTONE_THICKNESS);
    }

    protected static boolean isReplaceable(BlockState state) {
        return state.getBlockType().hasBlockTag(BlockTags.REPLACEABLE);
    }
}
