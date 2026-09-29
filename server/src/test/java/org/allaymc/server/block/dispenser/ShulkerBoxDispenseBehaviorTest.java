package org.allaymc.server.block.dispenser;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dispenser.DispenseResult;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.interfaces.BlockEntityHopper;
import org.allaymc.api.blockentity.interfaces.BlockEntityShulkerBox;
import org.allaymc.api.item.interfaces.ItemShulkerBoxStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3i;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.allaymc.api.block.property.type.BlockPropertyTypes.FACING_DIRECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AllayTestExtension.class)
class ShulkerBoxDispenseBehaviorTest {

    private Dimension dimension;
    private Vector3i origin;

    @BeforeEach
    void setUp() {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        dimension = spawn.dimension();
        origin = new Vector3i((int) spawn.x() + 96, (int) spawn.y() + 48, (int) spawn.z() + 96);
        for (var dx = -2; dx <= 2; dx++) {
            for (var dy = -2; dy <= 3; dy++) {
                for (var dz = -2; dz <= 2; dz++) {
                    var x = origin.x + dx;
                    var y = origin.y + dy;
                    var z = origin.z + dz;
                    dimension.getChunkManager().getOrLoadChunk(x >> 4, z >> 4).join();
                    dimension.setBlockState(x, y, z, BlockTypes.AIR.getDefaultState());
                }
            }
        }
    }

    @Test
    void placesShulkerFacingDispenserWhenBelowIsAir() {
        var item = ItemTypes.UNDYED_SHULKER_BOX.createItemStack();
        ((ItemShulkerBoxStack) item).setStoredItems(Map.of(0, ItemTypes.DIAMOND.createItemStack(3)));
        item.setCustomName("kutu");

        var result = new ShulkerBoxDispenseBehavior().dispense(new Block(dimension, origin), BlockFace.NORTH, item);

        assertTrue(result.succeeded());
        assertEquals(DispenseResult.success(), result);
        var target = BlockFace.NORTH.offsetPos(origin);
        assertEquals(BlockTypes.UNDYED_SHULKER_BOX, dimension.getBlockState(target).getBlockType());
        var shulker = assertInstanceOf(BlockEntityShulkerBox.class, dimension.getBlockEntity(target));
        assertEquals("kutu", shulker.getCustomName());
        assertEquals(3, shulker.getContainer().getItemStack(0).getCount());
        assertEquals(ItemTypes.DIAMOND, shulker.getContainer().getItemStack(0).getItemType());
        assertEquals((byte) BlockFace.NORTH.ordinal(), shulker.saveNBT().getByte("facing"));
    }

    @Test
    void placesShulkerFacingUpWhenBelowIsSolid() {
        var target = BlockFace.SOUTH.offsetPos(origin);
        dimension.setBlockState(BlockFace.DOWN.offsetPos(target), BlockTypes.STONE.getDefaultState());

        var result = new ShulkerBoxDispenseBehavior().dispense(new Block(dimension, origin), BlockFace.SOUTH, ItemTypes.WHITE_SHULKER_BOX.createItemStack());

        assertTrue(result.succeeded());
        assertEquals(BlockTypes.WHITE_SHULKER_BOX, dimension.getBlockState(target).getBlockType());
        var shulker = assertInstanceOf(BlockEntityShulkerBox.class, dimension.getBlockEntity(target));
        assertEquals((byte) BlockFace.UP.ordinal(), shulker.saveNBT().getByte("facing"));
    }

    @Test
    void doesNotReplaceSolidBlock() {
        var target = BlockFace.EAST.offsetPos(origin);
        dimension.setBlockState(target, BlockTypes.STONE.getDefaultState());

        var result = new ShulkerBoxDispenseBehavior().dispense(new Block(dimension, origin), BlockFace.EAST, ItemTypes.UNDYED_SHULKER_BOX.createItemStack());

        assertFalse(result.succeeded());
        assertEquals(BlockTypes.STONE, dimension.getBlockState(target).getBlockType());
    }

    @Test
    void pistonBreaksShulkerIntoHopper() {
        var hopperPos = new Vector3i(origin.x, origin.y, origin.z);
        var shulkerPos = new Vector3i(origin.x, origin.y + 1, origin.z);
        var pistonPos = new Vector3i(origin.x, origin.y + 2, origin.z);
        dimension.setBlockState(hopperPos, BlockTypes.HOPPER.getDefaultState());
        dimension.setBlockState(shulkerPos, BlockTypes.UNDYED_SHULKER_BOX.getDefaultState());
        var shulker = assertInstanceOf(BlockEntityShulkerBox.class, dimension.getBlockEntity(shulkerPos));
        shulker.getContainer().setItemStack(0, ItemTypes.DIAMOND.createItemStack(3));
        dimension.setBlockState(pistonPos, BlockTypes.PISTON.getDefaultState().setPropertyValue(FACING_DIRECTION, BlockFace.DOWN.ordinal()));
        dimension.setBlockState(pistonPos.x + 1, pistonPos.y, pistonPos.z, BlockTypes.REDSTONE_BLOCK.getDefaultState());

        var piston = new Block(dimension, pistonPos);
        piston.getBehavior().onScheduledUpdate(piston);

        assertEquals(BlockTypes.PISTON_ARM_COLLISION, dimension.getBlockState(shulkerPos).getBlockType());
        var hopper = assertInstanceOf(BlockEntityHopper.class, dimension.getBlockEntity(hopperPos));
        var picked = hopper.getContainer().getItemStack(0);
        assertEquals(ItemTypes.UNDYED_SHULKER_BOX, picked.getItemType());
        assertEquals(3, ((ItemShulkerBoxStack) picked).getStoredItems().get(0).getCount());
    }
}
