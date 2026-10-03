package org.allaymc.server.block.component;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.component.BlockLiquidBaseComponent;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.enums.PotentSulfurState;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.interfaces.BlockEntityPotentSulfur;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityArmorStand;
import org.allaymc.api.entity.interfaces.EntityEnderCrystal;
import org.allaymc.api.entity.interfaces.EntityFallingBlock;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPainting;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.interfaces.EntityProjectile;
import org.allaymc.api.entity.interfaces.EntityXpOrb;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.sound.PotentSulfurGeyserSound;
import org.allaymc.server.component.annotation.Dependency;
import org.joml.Vector3d;
import org.joml.Vector3ic;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.List;

import static org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl.computeDormantDurationSeconds;
import static org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl.computeEruptionDurationSeconds;

public class BlockPotentSulfurBaseComponentImpl extends BlockBaseComponentImpl {

    protected static final int MAX_WATER_HEIGHT = 4;
    protected static final int NOXIOUS_GAS_RADIUS = 3;
    protected static final int NOXIOUS_GAS_EFFECT_TICKS = 80;
    protected static final int TICKS_PER_SECOND = 20;
    protected static final int ACTIVITY_CHECK_RANGE_SQ = 24 * 24;
    protected static final double AABB_EPSILON = 0.00001;

    @Dependency
    protected BlockBlockEntityHolderComponent<BlockEntityPotentSulfur> blockEntityHolderComponent;

    public BlockPotentSulfurBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);
        syncEnvironment(new Block(newBlockState, oldBlock.getPosition()), true);
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
        syncEnvironment(block, false);
    }

    @Override
    public void onScheduledUpdate(Block block) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var column = analyzeWaterColumn(dimension, pos);
        if (column == null) {
            setDry(block);
            return;
        }

        var below = dimension.getBlockState(BlockFace.DOWN.offsetPos(pos));
        var blockEntity = blockEntityHolderComponent.getBlockEntity(pos);

        if (hasLavaSourceBelow(below)) {
            if (changeState(block, PotentSulfurState.CONTINUOUS)) {
                playGeyserSound(dimension, pos, column, true, false);
            }
            var nearby = hasNearbyActivity(dimension, pos);
            if (!nearby) {
                scheduleUpdate(dimension, pos, 15);
                return;
            }
            applyGeyserPush(dimension, pos, column);
            if (dimension.getWorld().getTick() % TICKS_PER_SECOND == 0) {
                playGeyserSound(dimension, pos, column, true, true);
            }
            scheduleUpdate(dimension, pos, 1);
            return;
        }

        if (below.getBlockType() == BlockTypes.MAGMA) {
            var state = getState(block);
            if (state != PotentSulfurState.DORMANT && state != PotentSulfurState.ERUPTING) {
                syncEnvironment(block, false);
                return;
            }
            tickMagmaGeyser(block, column, blockEntity);
            return;
        }

        var nearby = hasNearbyActivity(dimension, pos);
        if (nearby && canEmitNoxiousGas(dimension, pos, column)) {
            applyNoxiousGas(dimension, pos, column);
        }
        changeState(block, PotentSulfurState.WET);
        scheduleUpdate(dimension, pos, nearby ? TICKS_PER_SECOND : TICKS_PER_SECOND * 2);
    }

    protected void syncEnvironment(Block block, boolean isNewlyPlaced) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var column = analyzeWaterColumn(dimension, pos);
        var below = dimension.getBlockState(BlockFace.DOWN.offsetPos(pos));
        var blockEntity = blockEntityHolderComponent.getBlockEntity(pos);

        if (column == null) {
            setDry(block);
            return;
        }

        if (hasLavaSourceBelow(below)) {
            blockEntity.setCountdown(0);
            changeState(block, PotentSulfurState.CONTINUOUS);
            scheduleUpdate(dimension, pos, 1);
            return;
        }

        if (below.getBlockType() == BlockTypes.MAGMA) {
            var state = getState(block);
            if (isNewlyPlaced || state != PotentSulfurState.DORMANT && state != PotentSulfurState.ERUPTING) {
                blockEntity.setCountdown(computeDormantDurationSeconds(column.height(), pos.x(), pos.y(), pos.z()));
                changeState(block, PotentSulfurState.DORMANT);
            }
            scheduleUpdate(dimension, pos, TICKS_PER_SECOND);
            return;
        }

        blockEntity.setCountdown(0);
        if (hasNearbyActivity(dimension, pos) && canEmitNoxiousGas(dimension, pos, column)) {
            applyNoxiousGas(dimension, pos, column);
        }
        changeState(block, PotentSulfurState.WET);
        scheduleUpdate(dimension, pos, TICKS_PER_SECOND);
    }

    protected void setDry(Block block) {
        if (block.getDimension().getBlockEntity(block.getPosition()) instanceof BlockEntityPotentSulfur blockEntity) {
            blockEntity.setCountdown(0);
        }
        changeState(block, PotentSulfurState.DRY);
    }

    protected void tickMagmaGeyser(Block block, WaterColumn column, BlockEntityPotentSulfur blockEntity) {
        var countdown = blockEntity.getCountdown();
        if (countdown <= 0) {
            toggleMagmaGeyserPhase(block, column, blockEntity);
            return;
        }

        blockEntity.setCountdown(countdown - 1);

        var dimension = block.getDimension();
        var pos = block.getPosition();
        if (getState(block) == PotentSulfurState.ERUPTING) {
            applyGeyserPush(dimension, pos, column);
            playGeyserSound(dimension, pos, column, false, true);
        }

        scheduleUpdate(dimension, pos, TICKS_PER_SECOND);
    }

    protected void toggleMagmaGeyserPhase(Block block, WaterColumn column, BlockEntityPotentSulfur blockEntity) {
        var dimension = block.getDimension();
        var pos = block.getPosition();
        var height = column.height();

        if (getState(block) == PotentSulfurState.ERUPTING) {
            changeState(block, PotentSulfurState.DORMANT);
            blockEntity.setCountdown(computeDormantDurationSeconds(height, pos.x(), pos.y(), pos.z()));
        } else {
            changeState(block, PotentSulfurState.ERUPTING);
            blockEntity.setCountdown(computeEruptionDurationSeconds(height, pos.x(), pos.y(), pos.z()));
            playGeyserSound(dimension, pos, column, false, false);
            applyGeyserPush(dimension, pos, column);
        }

        scheduleUpdate(dimension, pos, TICKS_PER_SECOND);
    }

    protected static PotentSulfurState getState(Block block) {
        return block.getPropertyValue(BlockPropertyTypes.POTENT_SULFUR_STATE);
    }

    protected static boolean changeState(Block block, PotentSulfurState state) {
        if (getState(block) == state) {
            return false;
        }

        var newState = block.getBlockState().setPropertyValue(BlockPropertyTypes.POTENT_SULFUR_STATE, state);
        block.getDimension().setBlockState(block.getPosition(), newState, 0, true, true, false);
        return true;
    }

    protected static void scheduleUpdate(Dimension dimension, Vector3ic pos, int delay) {
        var blockUpdateManager = dimension.getBlockUpdateManager();
        if (delay > 1 && blockUpdateManager.hasScheduledBlockUpdate(pos)) {
            return;
        }
        blockUpdateManager.scheduleBlockUpdateInDelay(pos, delay);
    }

    protected static WaterColumn analyzeWaterColumn(Dimension dimension, Vector3ic pos) {
        var height = 0;
        for (var dy = 1; dy <= MAX_WATER_HEIGHT; dy++) {
            var water = getWater(dimension, pos.x(), pos.y() + dy, pos.z());
            if (water == null || !BlockLiquidBaseComponent.isSource(water)) {
                break;
            }
            height++;
        }

        if (height == 0) {
            return null;
        }

        return new WaterColumn(height, pos.y() + height);
    }

    protected static BlockState getWater(Dimension dimension, int x, int y, int z) {
        var liquid = dimension.getLiquid(x, y, z).right();
        return liquid != null && liquid.getBlockType().hasBlockTag(BlockTags.WATER) ? liquid : null;
    }

    protected static boolean hasLavaSourceBelow(BlockState below) {
        return below.getBlockType().hasBlockTag(BlockTags.LAVA) &&
               below.getBehavior() instanceof BlockLiquidBaseComponent &&
               BlockLiquidBaseComponent.isSource(below);
    }

    protected static boolean hasNearbyActivity(Dimension dimension, Vector3ic pos) {
        for (var player : dimension.getPlayers()) {
            var entity = player.getControlledEntity();
            if (entity == null) {
                continue;
            }

            var location = entity.getLocation();
            var dx = location.x() - pos.x();
            var dy = location.y() - pos.y();
            var dz = location.z() - pos.z();
            if (dx * dx + dy * dy + dz * dz <= ACTIVITY_CHECK_RANGE_SQ) {
                return true;
            }
        }
        return false;
    }

    protected static boolean canEmitNoxiousGas(Dimension dimension, Vector3ic pos, WaterColumn column) {
        for (var dx = -NOXIOUS_GAS_RADIUS; dx <= NOXIOUS_GAS_RADIUS; dx++) {
            for (var dz = -NOXIOUS_GAS_RADIUS; dz <= NOXIOUS_GAS_RADIUS; dz++) {
                if (isGasEmittingCell(dimension, pos.x() + dx, column.surfaceY(), pos.z() + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static void applyNoxiousGas(Dimension dimension, Vector3ic pos, WaterColumn column) {
        var surfaceY = column.surfaceY();
        var area = new AABBd(
                pos.x() - NOXIOUS_GAS_RADIUS,
                pos.y() + 1,
                pos.z() - NOXIOUS_GAS_RADIUS,
                pos.x() + NOXIOUS_GAS_RADIUS + 1,
                surfaceY + 1,
                pos.z() + NOXIOUS_GAS_RADIUS + 1
        );

        for (var entity : getNearbyEntities(dimension, area)) {
            if (!isLivingCreature(entity) || !isEntityInNoxiousGas(dimension, pos, entity, surfaceY)) {
                continue;
            }
            ((EntityLiving) entity).addEffect(EffectTypes.NAUSEA.createInstance(0, NOXIOUS_GAS_EFFECT_TICKS, true, false));
        }
    }

    protected static boolean isLivingCreature(Entity entity) {
        return entity instanceof EntityLiving &&
               !(entity instanceof EntityItem) &&
               !(entity instanceof EntityXpOrb) &&
               !(entity instanceof EntityProjectile) &&
               !(entity instanceof EntityPainting) &&
               !(entity instanceof EntityEnderCrystal) &&
               !(entity instanceof EntityArmorStand);
    }

    protected static boolean isEntityInNoxiousGas(Dimension dimension, Vector3ic pos, Entity entity, int surfaceY) {
        var aabb = entity.getOffsetAABB();
        if (aabb.maxY() < pos.y() + 1 || aabb.minY() > surfaceY + 2) {
            return false;
        }

        for (var dx = -NOXIOUS_GAS_RADIUS; dx <= NOXIOUS_GAS_RADIUS; dx++) {
            for (var dz = -NOXIOUS_GAS_RADIUS; dz <= NOXIOUS_GAS_RADIUS; dz++) {
                var gx = pos.x() + dx;
                var gz = pos.z() + dz;
                if (!isGasEmittingCell(dimension, gx, surfaceY, gz)) {
                    continue;
                }
                if (intersects(aabb, new AABBd(gx, surfaceY, gz, gx + 1, surfaceY + 2, gz + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean isGasEmittingCell(Dimension dimension, int x, int surfaceY, int z) {
        var water = getWater(dimension, x, surfaceY, z);
        if (water == null || !BlockLiquidBaseComponent.isSource(water)) {
            return false;
        }

        return dimension.getBlockState(x, surfaceY + 1, z).getBlockType().hasBlockTag(BlockTags.REPLACEABLE);
    }

    protected static void applyGeyserPush(Dimension dimension, Vector3ic pos, WaterColumn column) {
        if (!hasNearbyActivity(dimension, pos)) {
            return;
        }

        var surfaceY = column.surfaceY();
        var area = new AABBd(pos.x(), pos.y() + 1, pos.z(), pos.x() + 1, surfaceY + 1, pos.z() + 1);
        var push = 0.04 + 0.02 * column.height();

        for (var entity : getNearbyEntities(dimension, area)) {
            if (!canBeMovedByCurrents(entity) || !isEntityInWaterColumn(dimension, pos, entity, pos.y() + 1, surfaceY)) {
                continue;
            }

            var physics = (EntityPhysicsComponent) entity;
            physics.resetFallDistance();
            var motion = physics.getMotion();
            if (motion.y() < push) {
                physics.setMotion(new Vector3d(motion.x(), push, motion.z()));
            }
        }
    }

    protected static boolean canBeMovedByCurrents(Entity entity) {
        return entity instanceof EntityPhysicsComponent &&
               !(entity instanceof EntityPlayer) &&
               !(entity instanceof EntityFallingBlock);
    }

    protected static boolean isEntityInWaterColumn(Dimension dimension, Vector3ic pos, Entity entity, int minY, int maxY) {
        var location = entity.getLocation();
        var ex = (int) Math.floor(location.x());
        var ey = (int) Math.floor(location.y());
        var ez = (int) Math.floor(location.z());

        if (ex != pos.x() || ez != pos.z()) {
            return false;
        }
        if (ey < minY || ey > maxY) {
            return false;
        }

        return getWater(dimension, ex, ey, ez) != null;
    }

    protected static List<Entity> getNearbyEntities(Dimension dimension, AABBdc area) {
        return dimension.getEntityManager().getPhysicsService().computeCollidingEntities(
                area, entity -> intersects(entity.getOffsetAABB(), area)
        );
    }

    protected static boolean intersects(AABBdc a, AABBdc b) {
        return b.maxX() - a.minX() > AABB_EPSILON && a.maxX() - b.minX() > AABB_EPSILON &&
               b.maxY() - a.minY() > AABB_EPSILON && a.maxY() - b.minY() > AABB_EPSILON &&
               b.maxZ() - a.minZ() > AABB_EPSILON && a.maxZ() - b.minZ() > AABB_EPSILON;
    }

    protected static void playGeyserSound(Dimension dimension, Vector3ic pos, WaterColumn column, boolean continuous, boolean active) {
        dimension.addSound(
                new Vector3d(pos.x() + 0.5, column.surfaceY() + 0.5, pos.z() + 0.5),
                new PotentSulfurGeyserSound(continuous, active)
        );
    }

    protected record WaterColumn(int height, int surfaceY) {
    }
}
