package org.allaymc.api.container.interfaces;

public interface CrafterContainer extends BlockContainer, RecipeContainer, SidedContainer {

    int SIZE = 9;

    int getDisabledSlotsMask();

    void setDisabledSlotsMask(int mask);

    default boolean isSlotDisabled(int slot) {
        return slot >= 0 && slot < SIZE && (getDisabledSlotsMask() & (1 << slot)) != 0;
    }

    default void setSlotDisabled(int slot, boolean disabled) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }

        var mask = getDisabledSlotsMask();
        setDisabledSlotsMask(disabled ? mask | (1 << slot) : mask & ~(1 << slot));
    }

    default void toggleSlotDisabled(int slot) {
        setSlotDisabled(slot, !isSlotDisabled(slot));
    }
}
