package org.allaymc.server.block.component.piston;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.enums.MinecraftVerticalHalf;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.component.BlockEntityContainerHolderComponent;
import org.allaymc.api.blockentity.interfaces.BlockEntityHopper;
import org.allaymc.api.blockentity.interfaces.BlockEntityMovingBlock;
import org.allaymc.api.blockentity.interfaces.BlockEntityPistonArm;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.eventbus.event.block.BlockPistonEvent;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.particle.BlockBreakParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.block.component.BlockBaseComponentImpl;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.allaymc.api.block.property.type.BlockPropertyTypes.FACING_DIRECTION;
import static org.allaymc.api.block.property.type.BlockPropertyTypes.MINECRAFT_VERTICAL_HALF;
import static org.allaymc.api.block.property.type.BlockPropertyTypes.TOGGLE_BIT;

/**
 * Base component implementation for piston and sticky piston blocks.
 *
 * @author daoge_cmd
 */
@Slf4j
public class BlockPistonBaseComponentImpl extends BlockBaseComponentImpl {

    /**
     * Duration of the piston extension/retraction animation (2 game ticks = 100ms).
     */
    protected static final Duration ANIMATION_DURATION = Duration.ofMillis(100);

    public BlockPistonBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean place(Dimension dimension, BlockState blockState, Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        if (placementInfo != null) {
            // Calculate facing direction based on player position
            int facing = getPlacementFacing(placeBlockPos, placementInfo);
            blockState = blockState.setPropertyValue(FACING_DIRECTION, facing);
        }

        boolean result = dimension.setBlockState(
                placeBlockPos.x(), placeBlockPos.y(), placeBlockPos.z(),
                blockState, placementInfo
        );

        if (result) {
            // Note: Block entity is automatically created by BlockBlockEntityHolderComponent
            // We need to initialize the piston arm properties after it's created
            var pistonArm = (BlockEntityPistonArm) dimension.getBlockEntity(placeBlockPos);
            if (pistonArm != null) {
                pistonArm.setSticky(isSticky());
                pistonArm.setFacing(getPistonFace(blockState));

                // Check if we should extend immediately due to redstone power
                Block block = new Block(dimension, placeBlockPos);
                boolean isPowered = hasRedstonePower(block);
                pistonArm.setPowered(isPowered);

                if (isPowered) {
                    // Schedule extension for next tick
                    dimension.getBlockUpdateManager().scheduleBlockUpdateInDelay(placeBlockPos, Duration.ofMillis(50));
                }
            }
        }

        return result;
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
        // Schedule a check for next tick to handle redstone updates
        block.scheduleUpdateInDelay(Duration.ofMillis(50));
    }

    @Override
    public void onScheduledUpdate(Block block) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var blockState = block.getBlockState();

        // Get piston arm block entity to check animation state
        var pistonArmBlockEntity = dimension.getBlockEntity(pos);
        if (!(pistonArmBlockEntity instanceof BlockEntityPistonArm pistonArm)) {
            return;
        }

        boolean isPowered = hasRedstonePower(block);

        // Only allow state changes when:
        // 1. Animation is complete (state % 2 == 0)
        // 2. Power state has changed (powered != isPowered)
        // state 0 = retracted, 1 = extending, 2 = extended, 3 = retracting
        if (pistonArm.getState() % 2 == 0 && pistonArm.isPowered() != isPowered) {
            pistonArm.setPowered(isPowered);

            boolean isExtended = isExtended(dimension, pos, blockState);

            if (isPowered && !isExtended) {
                // Try to extend
                if (!extend(block)) {
                    // Extension failed, schedule another check
                    block.scheduleUpdateInDelay(Duration.ofMillis(50));
                }
            } else if (!isPowered && isExtended) {
                // Try to retract
                retract(block);
            }
            return;
        }

        // If animation is in progress, or power state hasn't changed but we need to retry
        // Schedule another check for failed extension attempts
        if (isPowered && !isExtended(dimension, pos, blockState) && pistonArm.getState() % 2 == 0) {
            if (!extend(block)) {
                block.scheduleUpdateInDelay(Duration.ofMillis(50));
            }
        }
    }

    /**
     * Attempt to extend the piston.
     *
     * @param block The piston block
     * @return true if extension started successfully
     */
    protected boolean extend(Block block) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var blockState = block.getBlockState();
        BlockFace facing = getPistonFace(blockState);

        // Calculate what blocks need to be moved
        PistonPushCalculator calculator = new PistonPushCalculator(
                dimension, pos, facing, isSticky(), true
        );

        if (!calculator.calculate()) {
            // Cannot extend - something is blocking
            return false;
        }

        // Create event
        List<Vector3ic> blocksToMove = new ArrayList<>(calculator.getBlocksToMove());
        List<Vector3ic> blocksToDestroy = new ArrayList<>(calculator.getBlocksToDestroy());
        Map<Vector3ic, BlockState> originalStates = new HashMap<>(calculator.getOriginalStates());

        BlockPistonEvent event = new BlockPistonEvent(
                block, facing, blocksToMove, blocksToDestroy, originalStates, true, isSticky()
        );

        if (Server.getInstance().getEventBus().callEvent(event).isCancelled()) {
            return false;
        }

        // Execute the push
        executePush(dimension, pos, facing, blocksToMove, blocksToDestroy, originalStates);

        // Play sound
        dimension.addSound(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, SimpleSound.PISTON_PUSH);

        return true;
    }

    /**
     * Attempt to retract the piston.
     *
     * @param block The piston block
     * @return true if retraction started successfully
     */
    protected boolean retract(Block block) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var blockState = block.getBlockState();
        BlockFace facing = getPistonFace(blockState);

        // For sticky pistons, calculate what blocks to pull
        List<Vector3ic> blocksToMove = new ArrayList<>();
        List<Vector3ic> blocksToDestroy = new ArrayList<>();
        Map<Vector3ic, BlockState> originalStates = new HashMap<>();

        if (isSticky()) {
            PistonPushCalculator calculator = new PistonPushCalculator(
                    dimension, pos, facing, true, false
            );

            if (calculator.calculate()) {
                blocksToMove.addAll(calculator.getBlocksToMove());
                blocksToDestroy.addAll(calculator.getBlocksToDestroy());
                originalStates.putAll(calculator.getOriginalStates());
            }
        }

        // Create event
        BlockPistonEvent event = new BlockPistonEvent(
                block, facing, blocksToMove, blocksToDestroy, originalStates, false, isSticky()
        );

        if (Server.getInstance().getEventBus().callEvent(event).isCancelled()) {
            return false;
        }

        // Execute the retraction
        executeRetract(dimension, pos, facing, blocksToMove, blocksToDestroy, originalStates);

        // Play sound
        dimension.addSound(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, SimpleSound.PISTON_PULL);

        return true;
    }

    /**
     * Execute the push operation.
     */
    protected void executePush(
            Dimension dimension,
            Vector3ic pistonPos,
            BlockFace facing,
            List<Vector3ic> blocksToMove,
            List<Vector3ic> blocksToDestroy,
            Map<Vector3ic, BlockState> originalStates
    ) {
        // 1. Prepare piston arm block entity state FIRST (sends initial state to client)
        // This must happen BEFORE MOVING_BLOCK entities are created
        var pistonArmBlockEntity = dimension.getBlockEntity(pistonPos);
        if (pistonArmBlockEntity instanceof BlockEntityPistonArm pistonArm) {
            pistonArm.preExtending(blocksToMove, originalStates);
        }

        destroyPushedBlocks(dimension, blocksToDestroy, originalStates);

        // 3. Save block entity NBT and prevent container items from dropping
        Map<Vector3ic, org.cloudburstmc.nbt.NbtMap> blockEntityNBTs = new HashMap<>();
        for (Vector3ic pos : blocksToMove) {
            var blockEntity = dimension.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntityNBTs.put(pos, blockEntity.saveCleanNBT());
                // Prevent container items from dropping when the block is moved
                if (blockEntity instanceof BlockEntityContainerHolderComponent containerHolder) {
                    containerHolder.setDropItemOnBreak(false);
                }
            }
        }

        // 4. Clear the original positions of blocks to move (from back to front)
        for (int i = blocksToMove.size() - 1; i >= 0; i--) {
            Vector3ic pos = blocksToMove.get(i);
            dimension.setBlockState(pos, BlockTypes.AIR.getDefaultState());
        }

        // 5. Place blocks in their new positions (from front to back)
        for (int i = 0; i < blocksToMove.size(); i++) {
            Vector3ic oldPos = blocksToMove.get(i);
            Vector3ic newPos = facing.offsetPos(oldPos);
            BlockState state = originalStates.get(oldPos);

            if (state != null) {
                // Create moving block at new position
                dimension.setBlockState(newPos, BlockTypes.MOVING_BLOCK.getDefaultState());
                // Initialize the MovingBlock block entity with the original block state
                var movingBlockEntity = dimension.getBlockEntity(newPos);
                if (movingBlockEntity instanceof BlockEntityMovingBlock movingBlock) {
                    movingBlock.setMovingBlockState(state);
                    movingBlock.setPistonPos(new Vector3i(pistonPos));
                    movingBlock.setExpanding(true); // Extending = expanding away from piston
                    // Restore block entity NBT if any
                    movingBlock.setMovingBlockEntityNBT(blockEntityNBTs.get(oldPos));
                }
            }
        }

        // 6. Create the piston arm at the extended position
        Vector3ic armPos = facing.offsetPos(pistonPos);
        BlockType<?> armType = isSticky()
                ? BlockTypes.STICKY_PISTON_ARM_COLLISION
                : BlockTypes.PISTON_ARM_COLLISION;
        // The piston arm's facing_direction should match the piston's raw stored value
        int armFacing = dimension.getBlockState(pistonPos).getPropertyValue(FACING_DIRECTION);
        dimension.setBlockState(armPos, armType.getDefaultState()
                .setPropertyValue(FACING_DIRECTION, armFacing));

        // 6. Start the actual movement
        if (pistonArmBlockEntity instanceof BlockEntityPistonArm pistonArm) {
            pistonArm.startMoving();
        }

        // 7. Schedule animation completion
        dimension.getBlockUpdateManager().scheduleBlockUpdateInDelay(pistonPos, ANIMATION_DURATION);
    }

    /**
     * Execute the retract operation.
     */
    protected void executeRetract(
            Dimension dimension,
            Vector3ic pistonPos,
            BlockFace facing,
            List<Vector3ic> blocksToMove,
            List<Vector3ic> blocksToDestroy,
            Map<Vector3ic, BlockState> originalStates
    ) {
        // 1. Prepare piston arm block entity state FIRST (sends initial state to client)
        // This must happen BEFORE MOVING_BLOCK entities are created
        var pistonArmBlockEntity = dimension.getBlockEntity(pistonPos);
        if (pistonArmBlockEntity instanceof BlockEntityPistonArm pistonArm) {
            pistonArm.preRetracting(blocksToMove, originalStates);
        }

        destroyPushedBlocks(dimension, blocksToDestroy, originalStates);

        // 3. For sticky piston, move blocks
        if (isSticky() && !blocksToMove.isEmpty()) {
            // Save block entity NBT and prevent container items from dropping
            Map<Vector3ic, org.cloudburstmc.nbt.NbtMap> blockEntityNBTs = new HashMap<>();
            for (Vector3ic pos : blocksToMove) {
                var blockEntity = dimension.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntityNBTs.put(pos, blockEntity.saveCleanNBT());
                    // Prevent container items from dropping when the block is moved
                    if (blockEntity instanceof BlockEntityContainerHolderComponent containerHolder) {
                        containerHolder.setDropItemOnBreak(false);
                    }
                }
            }

            // Clear original positions
            for (Vector3ic pos : blocksToMove) {
                dimension.setBlockState(pos, BlockTypes.AIR.getDefaultState());
            }

            // Place at new positions (one closer to piston) using MOVING_BLOCK for animation
            BlockFace pullDirection = facing.opposite();
            for (Vector3ic oldPos : blocksToMove) {
                Vector3ic newPos = pullDirection.offsetPos(oldPos);
                BlockState state = originalStates.get(oldPos);
                if (state != null) {
                    // Create moving block at new position for smooth animation
                    dimension.setBlockState(newPos, BlockTypes.MOVING_BLOCK.getDefaultState());
                    // Initialize the MovingBlock block entity
                    var movingBlockEntity = dimension.getBlockEntity(newPos);
                    if (movingBlockEntity instanceof BlockEntityMovingBlock movingBlock) {
                        movingBlock.setMovingBlockState(state);
                        movingBlock.setPistonPos(new Vector3i(pistonPos));
                        movingBlock.setExpanding(false); // Retracting = not expanding
                        // Restore block entity NBT if any
                        movingBlock.setMovingBlockEntityNBT(blockEntityNBTs.get(oldPos));
                    }
                }
            }
        } else {
            // For non-sticky piston or no blocks to move, just remove the piston arm
            Vector3ic armPos = facing.offsetPos(pistonPos);
            dimension.setBlockState(armPos, BlockTypes.AIR.getDefaultState());
        }

        // 4. Start the actual movement
        // Note: Piston arm removal for sticky piston with blocks is handled in finishAnimation()
        if (pistonArmBlockEntity instanceof BlockEntityPistonArm pistonArm) {
            pistonArm.startMoving();
        }

        // 6. Schedule animation completion
        dimension.getBlockUpdateManager().scheduleBlockUpdateInDelay(pistonPos, ANIMATION_DURATION);
    }

    /**
     * Check if the piston has redstone power.
     */
    protected boolean hasRedstonePower(Block block) {
        BlockFace pistonFace = getPistonFace(block.getBlockState());

        // Check power from all sides except the face the piston is pointing
        // Piston should ignore power from the front face (the direction it extends towards)
        return block.getWeakPower(pistonFace) > 0 || block.getStrongPower(pistonFace) > 0;
    }

    /**
     * Check if the piston is currently extended.
     */
    protected boolean isExtended(Dimension dimension, Vector3ic pistonPos, BlockState pistonState) {
        BlockFace facing = getPistonFace(pistonState);
        Vector3ic armPos = facing.offsetPos(pistonPos);
        BlockState armState = dimension.getBlockState(armPos);
        BlockType<?> armType = armState.getBlockType();

        return armType == BlockTypes.PISTON_ARM_COLLISION ||
               armType == BlockTypes.STICKY_PISTON_ARM_COLLISION;
    }

    /**
     * Get the facing direction of the piston (the direction it pushes towards).
     * Note: For horizontal pistons, Minecraft stores the opposite direction in the
     * facing_direction property, so we need to invert it.
     */
    protected BlockFace getPistonFace(BlockState blockState) {
        int facing = blockState.getPropertyValue(FACING_DIRECTION);
        BlockFace face = BlockFace.fromIndex(facing);
        // For horizontal pistons, the stored direction is opposite to the push direction
        return face.isHorizontal() ? face.opposite() : face;
    }

    /**
     * Calculate the facing direction property value when placing the piston.
     * Note: For horizontal pistons, Minecraft stores the opposite of the push direction.
     */
    protected int getPlacementFacing(Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        var player = placementInfo.player();
        var playerPos = player.getLocation();

        // Calculate the horizontal distance
        double dx = playerPos.x() - (placeBlockPos.x() + 0.5);
        double dz = playerPos.z() - (placeBlockPos.z() + 0.5);
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // If player is very close, use vertical facing based on eye position
        if (horizontalDist < 2.0) {
            double dy = playerPos.y() + player.getEyeHeight() - (placeBlockPos.y() + 0.5);

            if (dy > 2.0) {
                return BlockFace.UP.ordinal();
            } else if (dy < 0) {
                return BlockFace.DOWN.ordinal();
            }
        }

        // For horizontal pistons, store the opposite direction (this is how Minecraft works)
        BlockFace horizontalFace = player.getHorizontalFace();
        return horizontalFace.ordinal();
    }

    @Override
    public void onBreak(Block block, ItemStack usedItem, Entity entity, List<ItemStack> drops) {
        super.onBreak(block, usedItem, entity, drops);

        // Only cascade when directly broken by player/entity, not when broken by code
        // This prevents infinite recursion between piston and piston arm
        if (entity == null) {
            return;
        }

        // When the piston is broken, also break the piston arm if extended
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var blockState = block.getBlockState();
        BlockFace facing = getPistonFace(blockState);
        Vector3ic armPos = facing.offsetPos(pos);

        var armState = dimension.getBlockState(armPos);
        if (armState.getBlockType() == BlockTypes.PISTON_ARM_COLLISION ||
            armState.getBlockType() == BlockTypes.STICKY_PISTON_ARM_COLLISION) {
            // Check that the arm's facing matches the piston's facing
            int armFacing = armState.getPropertyValue(FACING_DIRECTION);
            BlockFace armFace = BlockFace.fromIndex(armFacing);
            // For horizontal, the stored value is opposite
            if (armFace.isHorizontal()) {
                armFace = armFace.opposite();
            }
            if (armFace == facing) {
                // Use breakBlock with null entity to trigger particles/sound but prevent recursion
                dimension.breakBlock(armPos, null, null);
            }
        }
    }

    /**
     * Whether this piston is sticky.
     */
    protected boolean isSticky() {
        return false;
    }

    private void destroyPushedBlocks(Dimension dimension, List<Vector3ic> blocksToDestroy, Map<Vector3ic, BlockState> originalStates) {
        for (Vector3ic pos : blocksToDestroy) {
            if (originalStates.get(pos) == null) {
                continue;
            }
            var state = dimension.getBlockState(pos);
            if (state.getBlockType().getIdentifier().path().endsWith("shulker_box")) {
                breakShulkerBox(dimension, pos, state);
            } else {
                dimension.breakBlock(pos, null, null);
            }
        }
    }

    private void breakShulkerBox(Dimension dimension, Vector3ic pos, BlockState state) {
        var block = new Block(state, new Position3i(pos, dimension), 0);
        var drops = new ArrayList<ItemStack>();
        for (var drop : state.getBehavior().getDrops(block, null, null)) {
            if (drop != null && !drop.isEmptyOrAir()) {
                drops.add(drop.copy());
            }
        }
        dimension.addParticle(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, new BlockBreakParticle(state));
        dimension.setBlockState(pos, BlockTypes.AIR.getDefaultState());

        var dropPos = hopperFriendlyDropPos(dimension, pos);
        wakeHoppersAround(dimension, pos);
        var motion = new Vector3d(0, 0, 0);
        for (var drop : drops) {
            if (!offerDropToHopper(dimension, pos.x(), pos.y() - 1, pos.z(), dropPos, drop)
                    && !offerDropToHopper(dimension, pos.x(), pos.y() - 2, pos.z(), dropPos, drop)) {
                dimension.dropItem(drop, dropPos, motion, 0);
            }
        }
    }

    private boolean offerDropToHopper(Dimension dimension, int x, int y, int z, Vector3d dropPos, ItemStack drop) {
        if (!dimension.isInWorld(x, y, z) || drop.isEmptyOrAir()) {
            return false;
        }
        var state = dimension.getBlockState(x, y, z);
        if (state.getBlockType() != BlockTypes.HOPPER || state.getPropertyValue(TOGGLE_BIT)) {
            return false;
        }
        if (dropPos.x() < x || dropPos.x() >= x + 1 || dropPos.y() < y || dropPos.y() >= y + 2 || dropPos.z() < z || dropPos.z() >= z + 1) {
            return false;
        }
        if (!(dimension.getBlockEntity(x, y, z) instanceof BlockEntityHopper hopper) || hopper.getContainer().isFull()) {
            return false;
        }
        if (hopper.getContainer().tryAddItem(drop) == -1) {
            return false;
        }
        hopper.setTransferCooldown(8);
        return true;
    }

    private Vector3d hopperFriendlyDropPos(Dimension dimension, Vector3ic pos) {
        var belowPos = BlockFace.DOWN.offsetPos(pos);
        if (!dimension.isInWorld(belowPos.x(), belowPos.y(), belowPos.z())) {
            return new Vector3d(pos.x() + 0.5, pos.y() + 0.25, pos.z() + 0.5);
        }

        var below = dimension.getBlockState(belowPos);
        if (below.getBlockType() == BlockTypes.HOPPER) {
            return new Vector3d(pos.x() + 0.5, pos.y() + 0.25, pos.z() + 0.5);
        }
        if (below.getBlockType().hasProperty(MINECRAFT_VERTICAL_HALF)) {
            double surfaceY = below.getPropertyValue(MINECRAFT_VERTICAL_HALF) == MinecraftVerticalHalf.BOTTOM ? 0.51 : 0.01;
            return new Vector3d(belowPos.x() + 0.5, belowPos.y() + surfaceY, belowPos.z() + 0.5);
        }
        if (below.getBlockType() == BlockTypes.SOUL_SAND || below.getBlockType() == BlockTypes.MUD) {
            return new Vector3d(belowPos.x() + 0.5, belowPos.y() + 0.9, belowPos.z() + 0.5);
        }
        return new Vector3d(pos.x() + 0.5, pos.y() + 0.25, pos.z() + 0.5);
    }

    private void wakeHoppersAround(Dimension dimension, Vector3ic pos) {
        int x = pos.x();
        int y = pos.y();
        int z = pos.z();
        wakeHopperAt(dimension, x, y - 1, z, null);
        wakeHopperAt(dimension, x, y - 2, z, null);
        wakeHopperAt(dimension, x, y + 1, z, BlockFace.DOWN);
        wakeHopperAt(dimension, x, y, z + 1, BlockFace.NORTH);
        wakeHopperAt(dimension, x, y, z - 1, BlockFace.SOUTH);
        wakeHopperAt(dimension, x + 1, y, z, BlockFace.WEST);
        wakeHopperAt(dimension, x - 1, y, z, BlockFace.EAST);
    }

    private void wakeHopperAt(Dimension dimension, int x, int y, int z, BlockFace requiredFacing) {
        if (!dimension.isInWorld(x, y, z)) {
            return;
        }
        var state = dimension.getBlockState(x, y, z);
        if (state.getBlockType() != BlockTypes.HOPPER) {
            return;
        }
        if (requiredFacing != null && BlockFace.fromIndex(state.getPropertyValue(FACING_DIRECTION)) != requiredFacing) {
            return;
        }
        if (dimension.getBlockEntity(x, y, z) instanceof BlockEntityHopper hopper) {
            hopper.setTransferCooldown(0);
        }
    }
}
