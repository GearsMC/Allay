package org.allaymc.server.container;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.item.recipe.input.CraftingRecipeInput;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.container.impl.CrafterContainerImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crafter kabinin devre disi slot maskesini, huninin kullandigi slot sirasini
 * ve karsilastirici sinyalini dogrular.
 */
@ExtendWith(AllayTestExtension.class)
class CrafterContainerTest {

    private static CrafterContainerImpl container() {
        return new CrafterContainerImpl();
    }

    @Test
    void disabledSlotsAreMaskedToNineBits() {
        var container = container();

        container.setDisabledSlotsMask(0xffff);

        assertEquals(0x1ff, container.getDisabledSlotsMask());
        assertTrue(container.isSlotDisabled(8));
        assertFalse(container.isSlotDisabled(9));
    }

    @Test
    void togglingASlotOnlyFlipsThatBit() {
        var container = container();

        container.toggleSlotDisabled(3);
        container.toggleSlotDisabled(5);
        container.toggleSlotDisabled(3);

        assertEquals(1 << 5, container.getDisabledSlotsMask());
    }

    @Test
    void disabledSlotsAreExcludedFromTheRecipeInput() {
        var container = container();
        container.setItemStack(0, ItemTypes.STICK.createItemStack(4));
        container.setItemStack(1, ItemTypes.STICK.createItemStack(4));
        container.setSlotDisabled(1, true);

        var input = (CraftingRecipeInput) container.createRecipeInput();

        assertEquals(1, input.items()[0][0].getCount());
        assertSame(ItemAirStack.AIR_STACK, input.items()[0][1]);
    }

    @Test
    void hopperInsertsIntoTheLeastFilledEnabledSlot() {
        var container = container();
        container.setItemStack(0, ItemTypes.STICK.createItemStack(5));
        container.setItemStack(1, ItemTypes.STICK.createItemStack(2));
        container.setItemStack(2, ItemTypes.COBBLESTONE.createItemStack(1));
        container.setSlotDisabled(3, true);

        var allowed = container.getAllowedInsertSlots(BlockFace.UP, ItemTypes.STICK.createItemStack(1));

        assertArrayEquals(new int[]{4, 5, 6, 7, 8, 1, 0}, allowed);
    }

    @Test
    void hopperCannotExtractFromCrafter() {
        var container = container();
        container.setItemStack(0, ItemTypes.STICK.createItemStack(1));

        assertArrayEquals(new int[0], container.getAllowedExtractSlots(BlockFace.DOWN));
        assertArrayEquals(new int[0], container.getAllowedExtractSlots(BlockFace.UP));
    }

    @Test
    void comparatorCountsFilledAndDisabledSlots() {
        var container = container();
        container.setItemStack(0, ItemTypes.STICK.createItemStack(1));
        container.setItemStack(1, ItemTypes.STICK.createItemStack(1));
        container.setSlotDisabled(8, true);

        assertEquals(3, container.calculateComparatorSignal());
    }
}
