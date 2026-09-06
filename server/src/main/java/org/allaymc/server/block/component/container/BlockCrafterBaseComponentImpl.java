package org.allaymc.server.block.component.container;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.enums.Orientation;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.blockentity.interfaces.BlockEntityCrafter;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.world.Dimension;
import org.allaymc.server.block.component.BlockBaseComponentImpl;
import org.allaymc.server.component.annotation.Dependency;
import org.joml.Vector3ic;

import java.time.Duration;

public class BlockCrafterBaseComponentImpl extends BlockBaseComponentImpl {

    protected static final Duration TRIGGER_DELAY = Duration.ofMillis(200);
    protected static final Duration CONTINUE_CRAFT_DELAY = Duration.ofMillis(50);

    @Dependency
    protected BlockBlockEntityHolderComponent<BlockEntityCrafter> blockEntityHolderComponent;

    public BlockCrafterBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean place(Dimension dimension, BlockState blockState, Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        if (placementInfo == null || placementInfo.player() == null) {
            return dimension.setBlockState(placeBlockPos.x(), placeBlockPos.y(), placeBlockPos.z(), blockState);
        }

        var processedState = processBlockProperties(blockState, placeBlockPos, placementInfo)
                .setPropertyValue(BlockPropertyTypes.ORIENTATION, orientationFromPlayer(placementInfo.player()));

        return dimension.setBlockState(placeBlockPos.x(), placeBlockPos.y(), placeBlockPos.z(), processedState, placementInfo);
    }

    public static Orientation orientationFromPlayer(EntityPlayer player) {
        var pitch = player.getLocation().pitch();
        BlockFace primary;
        if (pitch > 45) {
            primary = BlockFace.UP;
        } else if (pitch < -45) {
            primary = BlockFace.DOWN;
        } else {
            primary = player.getHorizontalFace().opposite();
        }

        var secondary = BlockFace.UP;
        if (primary == BlockFace.UP || primary == BlockFace.DOWN) {
            secondary = player.getHorizontalFace().opposite();
        }

        return orientationFromFaces(primary, secondary);
    }

    public static Orientation orientationFromFaces(BlockFace primary, BlockFace secondary) {
        return switch (primary) {
            case DOWN -> switch (secondary) {
                case NORTH -> Orientation.DOWN_NORTH;
                case SOUTH -> Orientation.DOWN_SOUTH;
                case WEST -> Orientation.DOWN_WEST;
                default -> Orientation.DOWN_EAST;
            };
            case UP -> switch (secondary) {
                case NORTH -> Orientation.UP_NORTH;
                case SOUTH -> Orientation.UP_SOUTH;
                case WEST -> Orientation.UP_WEST;
                default -> Orientation.UP_EAST;
            };
            case EAST -> secondary == BlockFace.UP ? Orientation.EAST_UP : Orientation.DOWN_EAST;
            case WEST -> secondary == BlockFace.UP ? Orientation.WEST_UP : Orientation.DOWN_EAST;
            case NORTH -> secondary == BlockFace.UP ? Orientation.NORTH_UP : Orientation.DOWN_EAST;
            case SOUTH -> secondary == BlockFace.UP ? Orientation.SOUTH_UP : Orientation.DOWN_EAST;
        };
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);
        updateRedstoneState(new Block(oldBlock.getDimension(), oldBlock.getPosition()));
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
        updateRedstoneState(block);
    }

    protected void updateRedstoneState(Block block) {
        var blockEntity = blockEntityHolderComponent.getBlockEntity(block.getPosition());
        if (blockEntity == null) {
            return;
        }

        var powered = block.isPowered();
        var triggered = block.getPropertyValue(BlockPropertyTypes.TRIGGERED_BIT);

        if (powered) {
            if (blockEntity.isAwaitingPowerRelease()) {
                if (!triggered) {
                    block.updateBlockProperty(BlockPropertyTypes.TRIGGERED_BIT, true);
                }
                if (blockEntity.hasPendingCraft()) {
                    block.scheduleUpdateInDelay(TRIGGER_DELAY);
                }
                return;
            }

            blockEntity.setAwaitingPowerRelease(true);
            blockEntity.setPendingCraft(true);
            block.updateBlockProperty(BlockPropertyTypes.TRIGGERED_BIT, true);
            block.scheduleUpdateInDelay(TRIGGER_DELAY);
            return;
        }

        blockEntity.setAwaitingPowerRelease(false);
        if (triggered && !blockEntity.hasPendingCraft()) {
            block.updateBlockProperty(BlockPropertyTypes.TRIGGERED_BIT, false);
        }
    }

    @Override
    public void onScheduledUpdate(Block block) {
        var blockEntity = blockEntityHolderComponent.getBlockEntity(block.getPosition());
        if (blockEntity == null) {
            return;
        }

        var triggered = block.getPropertyValue(BlockPropertyTypes.TRIGGERED_BIT);
        var crafting = block.getPropertyValue(BlockPropertyTypes.CRAFTING);
        var shouldCraft = triggered || blockEntity.hasPendingCraft();

        if (shouldCraft && !crafting) {
            blockEntity.setPendingCraft(false);

            var crafted = blockEntity.tryCraftBatch(getAdaptiveCraftBatchLimit(block));
            var stillPowered = block.isPowered();

            if (crafted > 0 && blockEntity.canCraftMore()) {
                updateState(block, stillPowered, true);
                block.scheduleUpdateInDelay(CONTINUE_CRAFT_DELAY);
                notifyNeighbors(block);
                return;
            }

            updateState(block, stillPowered, crafted > 0);
            if (crafted > 0) {
                block.scheduleUpdateInDelay(TRIGGER_DELAY);
            }
            notifyNeighbors(block);
            return;
        }

        if (crafting) {
            var crafted = blockEntity.tryCraftBatch(getAdaptiveCraftBatchLimit(block));
            if (crafted > 0 && blockEntity.canCraftMore()) {
                block.scheduleUpdateInDelay(CONTINUE_CRAFT_DELAY);
                notifyNeighbors(block);
                return;
            }

            updateState(block, block.isPowered(), false);
            notifyNeighbors(block);
        }
    }

    protected void updateState(Block block, boolean triggered, boolean crafting) {
        block.updateBlockProperty(BlockPropertyTypes.TRIGGERED_BIT, triggered);
        block.updateBlockProperty(BlockPropertyTypes.CRAFTING, crafting);
    }

    protected void notifyNeighbors(Block block) {
        block.getDimension().updateComparatorOutputLevel(block.getPosition());
    }

    protected int getAdaptiveCraftBatchLimit(Block block) {
        var tps = block.getDimension().getWorld().getTPS();
        if (tps >= 18.0f) {
            return 64;
        }
        if (tps >= 15.0f) {
            return 32;
        }
        if (tps >= 12.0f) {
            return 16;
        }

        return 8;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        var blockEntity = blockEntityHolderComponent.getBlockEntity(block.getPosition());
        return blockEntity == null ? 0 : blockEntity.getContainer().calculateComparatorSignal();
    }
}
