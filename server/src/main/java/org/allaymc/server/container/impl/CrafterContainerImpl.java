package org.allaymc.server.container.impl;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.container.interfaces.CrafterContainer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.item.recipe.input.CraftingRecipeInput;
import org.allaymc.api.item.recipe.input.RecipeInput;

import java.util.ArrayList;
import java.util.Comparator;

public class CrafterContainerImpl extends BlockContainerImpl implements CrafterContainer {

    protected int disabledSlots;

    public CrafterContainerImpl() {
        super(ContainerTypes.CRAFTER);
    }

    @Override
    public int getDisabledSlotsMask() {
        return disabledSlots;
    }

    @Override
    public void setDisabledSlotsMask(int mask) {
        this.disabledSlots = mask & 0x1ff;
    }

    @Override
    public RecipeInput createRecipeInput() {
        return new CraftingRecipeInput(
                pickOne(0), pickOne(1), pickOne(2),
                pickOne(3), pickOne(4), pickOne(5),
                pickOne(6), pickOne(7), pickOne(8)
        );
    }

    @Override
    public int[] getAllowedInsertSlots(BlockFace side, ItemStack stack) {
        var candidates = new ArrayList<Integer>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            if (isSlotDisabled(slot)) {
                continue;
            }

            var existing = getItemStack(slot);
            if (existing == ItemAirStack.AIR_STACK) {
                candidates.add(slot);
                continue;
            }

            if (existing.canMerge(stack, true) && !existing.isFull()) {
                candidates.add(slot);
            }
        }

        candidates.sort(Comparator.comparingInt(this::countIn));
        return candidates.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public int[] getAllowedExtractSlots(BlockFace side) {
        return null;
    }

    @Override
    public int calculateComparatorSignal() {
        int filled = 0;
        for (int slot = 0; slot < SIZE; slot++) {
            if (isSlotDisabled(slot) || !isEmpty(slot)) {
                filled++;
            }
        }
        return filled;
    }

    protected int countIn(int slot) {
        var stack = getItemStack(slot);
        return stack == ItemAirStack.AIR_STACK ? 0 : stack.getCount();
    }

    protected ItemStack pickOne(int slot) {
        if (isSlotDisabled(slot) || isEmpty(slot)) {
            return ItemAirStack.AIR_STACK;
        }

        var copy = getItemStack(slot).copy(false);
        copy.setCount(1);
        return copy;
    }
}
