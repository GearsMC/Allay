package org.allaymc.api.blockentity.component;

public interface BlockEntityCrafterBaseComponent extends BlockEntityBaseComponent {

    boolean isAwaitingPowerRelease();

    void setAwaitingPowerRelease(boolean awaitingPowerRelease);

    boolean hasPendingCraft();

    void setPendingCraft(boolean pendingCraft);

    boolean tryCraft();

    int tryCraftBatch(int maxCrafts);

    boolean canCraftMore();

    void syncRecipePreviewToViewers();

    void syncSlotMaskToViewers();
}
