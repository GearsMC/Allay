package org.allaymc.server.blockentity;

import org.allaymc.api.block.interfaces.BlockCrafterBehavior;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.interfaces.BlockEntityChest;
import org.allaymc.api.blockentity.interfaces.BlockEntityCrafter;
import org.allaymc.api.blockentity.interfaces.BlockEntityFurnace;
import org.allaymc.api.container.interfaces.CrafterContainer;
import org.allaymc.api.container.interfaces.FurnaceContainer;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crafter'in gercek bir dunyada uretim yaptigini dogrular.
 */
@ExtendWith(AllayTestExtension.class)
class CrafterCraftTest {

    private static Dimension dimension() {
        return Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
    }

    private static Vector3i near(int dx, int dy, int dz) {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        return new Vector3i(spawn.x() + dx, spawn.y() + dy, spawn.z() + dz);
    }

    private static void loadChunk(Dimension dimension, Vector3i pos) {
        if (dimension.getChunkManager().getChunkByDimensionPos(pos.x(), pos.z()) == null) {
            dimension.getChunkManager().getOrLoadChunk(pos.x() >> 4, pos.z() >> 4).join();
        }
    }

    @Test
    void crafterCraftsABatchAndFillsTheChestBelow() {
        var dimension = dimension();
        var crafterPos = near(0, 4, 0);
        var chestPos = near(0, 3, 0);
        loadChunk(dimension, crafterPos);

        assertTrue(dimension.setBlockState(chestPos, BlockTypes.CHEST.getDefaultState()));
        assertTrue(dimension.setBlockState(crafterPos, BlockTypes.CRAFTER.getDefaultState()));

        var crafter = crafterAt(dimension, crafterPos);
        var chest = assertInstanceOf(BlockEntityChest.class,
                BlockTypes.CHEST.getBlockBehavior().getBlockEntity(chestPos.x(), chestPos.y(), chestPos.z(), dimension));

        CrafterContainer container = crafter.getContainer();
        for (int slot = 1; slot < CrafterContainer.SIZE; slot++) {
            container.setSlotDisabled(slot, true);
        }
        container.setItemStack(0, ItemTypes.OAK_LOG.createItemStack(8));

        assertTrue(crafter.canCraftMore(), "tek etkin slotta kutuk varken uretim mumkun olmali");

        var crafted = crafter.tryCraftBatch(8);

        assertEquals(8, crafted, "sekiz kutuk tek cagrida islenmeli");
        assertTrue(container.isEmpty(0), "girdi slotu tuketilmeli");
        assertEquals(32, countIn(chest, ItemTypes.OAK_PLANKS.createItemStack().getItemType().getIdentifier().toString()),
                "sekiz uretim sandiga 32 tahta birakmali");
        assertEquals(0, crafter.tryCraftBatch(8), "girdi bitince uretim durmali");

        dimension.setBlockState(crafterPos, BlockTypes.AIR.getDefaultState());
        dimension.setBlockState(chestPos, BlockTypes.AIR.getDefaultState());
    }

    @Test
    void crafterWithoutARecipeDoesNotConsumeAnything() {
        var dimension = dimension();
        var crafterPos = near(1, 4, 1);
        loadChunk(dimension, crafterPos);

        assertTrue(dimension.setBlockState(crafterPos, BlockTypes.CRAFTER.getDefaultState()));
        var crafter = crafterAt(dimension, crafterPos);

        CrafterContainer container = crafter.getContainer();
        for (int slot = 2; slot < CrafterContainer.SIZE; slot++) {
            container.setSlotDisabled(slot, true);
        }
        container.setItemStack(0, ItemTypes.OAK_LOG.createItemStack(4));
        container.setItemStack(1, ItemTypes.DIAMOND_SWORD.createItemStack(1));

        assertEquals(0, crafter.tryCraftBatch(4));
        assertEquals(4, container.getItemStack(0).getCount(), "eslesme yoksa girdi tuketilmemeli");

        dimension.setBlockState(crafterPos, BlockTypes.AIR.getDefaultState());
    }

    @Test
    void crafterOutputRespectsFurnaceInsertSlots() {
        var dimension = dimension();
        var crafterPos = near(2, 4, 2);
        var furnacePos = near(2, 3, 2);
        loadChunk(dimension, crafterPos);

        assertTrue(dimension.setBlockState(furnacePos, BlockTypes.FURNACE.getDefaultState()));
        assertTrue(dimension.setBlockState(crafterPos, BlockTypes.CRAFTER.getDefaultState()));

        var crafter = crafterAt(dimension, crafterPos);
        var furnace = assertInstanceOf(BlockEntityFurnace.class,
                BlockTypes.FURNACE.getBlockBehavior().getBlockEntity(furnacePos.x(), furnacePos.y(), furnacePos.z(), dimension));

        CrafterContainer container = crafter.getContainer();
        for (int slot = 1; slot < CrafterContainer.SIZE; slot++) {
            container.setSlotDisabled(slot, true);
        }
        container.setItemStack(0, ItemTypes.OAK_LOG.createItemStack(1));

        assertEquals(1, crafter.tryCraftBatch(1));

        var furnaceContainer = (FurnaceContainer) furnace.getContainer();
        assertEquals(ItemTypes.OAK_PLANKS, furnaceContainer.getIngredient().getItemType());
        assertEquals(4, furnaceContainer.getIngredient().getCount());
        assertTrue(furnaceContainer.isEmpty(FurnaceContainer.FUEL_SLOT));
        assertTrue(furnaceContainer.isEmpty(FurnaceContainer.RESULT_SLOT));

        dimension.setBlockState(crafterPos, BlockTypes.AIR.getDefaultState());
        dimension.setBlockState(furnacePos, BlockTypes.AIR.getDefaultState());
    }

    private static BlockEntityCrafter crafterAt(Dimension dimension, Vector3i pos) {
        var behavior = (BlockCrafterBehavior) BlockTypes.CRAFTER.getBlockBehavior();
        return assertInstanceOf(BlockEntityCrafter.class, behavior.getBlockEntity(pos.x(), pos.y(), pos.z(), dimension));
    }

    private static int countIn(BlockEntityChest chest, String itemIdentifier) {
        var total = 0;
        for (var stack : chest.getContainer().getItemStacks()) {
            if (stack.getItemType().getIdentifier().toString().equals(itemIdentifier)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
