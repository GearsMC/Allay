package org.allaymc.server.blockentity.component;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.property.enums.Orientation;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityContainerHolderComponent;
import org.allaymc.api.blockentity.component.BlockEntityCrafterBaseComponent;
import org.allaymc.api.blockentity.interfaces.BlockEntityChest;
import org.allaymc.api.container.Container;
import org.allaymc.api.container.interfaces.CrafterContainer;
import org.allaymc.api.container.interfaces.SidedContainer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.item.recipe.Recipe;
import org.allaymc.api.item.recipe.input.CraftingRecipeInput;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.api.world.particle.ShootParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.component.annotation.OnInitFinish;
import org.allaymc.server.item.recipe.CraftingRecipeMatcher;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BlockEntityCrafterBaseComponentImpl extends BlockEntityBaseComponentImpl implements BlockEntityCrafterBaseComponent {

    protected static final String TAG_DISABLED_SLOTS = "disabled_slots";
    protected static final String TAG_CRAFTING_TICKS_REMAINING = "crafting_ticks_remaining";

    @Dependency
    protected BlockEntityContainerHolderComponent containerHolderComponent;

    protected boolean awaitingPowerRelease;
    protected boolean pendingCraft;
    protected Recipe lastRecipe;
    protected long lastPreviewSyncTick = -1;

    public BlockEntityCrafterBaseComponentImpl(BlockEntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    @OnInitFinish
    public void onInitFinish(BlockEntityInitInfo initInfo) {
        super.onInitFinish(initInfo);

        var container = getContainer();
        container.addOpenListener(viewer -> syncRecipePreviewToViewers());
        for (int slot = 0; slot < CrafterContainer.SIZE; slot++) {
            container.addSlotChangeListener(slot, itemStack -> syncRecipePreviewOnSlotChange());
        }
    }

    protected void syncRecipePreviewOnSlotChange() {
        var tick = position.dimension().getWorld().getTick();
        if (lastPreviewSyncTick == tick) {
            return;
        }

        lastPreviewSyncTick = tick;
        syncRecipePreviewToViewers();
    }

    @Override
    public NbtMap saveNBT() {
        return super.saveNBT().toBuilder()
                .putShort(TAG_DISABLED_SLOTS, (short) getContainer().getDisabledSlotsMask())
                .putInt(TAG_CRAFTING_TICKS_REMAINING, 0)
                .build();
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForShort(TAG_DISABLED_SLOTS, value -> getContainer().setDisabledSlotsMask(value));
        nbt.listenForInt(TAG_DISABLED_SLOTS, value -> getContainer().setDisabledSlotsMask(value));
    }

    @Override
    public boolean isAwaitingPowerRelease() {
        return awaitingPowerRelease;
    }

    @Override
    public void setAwaitingPowerRelease(boolean awaitingPowerRelease) {
        this.awaitingPowerRelease = awaitingPowerRelease;
    }

    @Override
    public boolean hasPendingCraft() {
        return pendingCraft;
    }

    @Override
    public void setPendingCraft(boolean pendingCraft) {
        this.pendingCraft = pendingCraft;
    }

    @Override
    public boolean tryCraft() {
        return tryCraftBatch(1) > 0;
    }

    @Override
    public int tryCraftBatch(int maxCrafts) {
        if (maxCrafts < 1 || !isCrafterBlock()) {
            return 0;
        }

        var container = getContainer();
        if (!areAllEnabledSlotsFilled(container)) {
            playFailSound();
            return 0;
        }

        var input = (CraftingRecipeInput) container.createRecipeInput();
        var recipe = CraftingRecipeMatcher.match(input, lastRecipe);
        if (recipe == null) {
            lastRecipe = null;
            playFailSound();
            return 0;
        }
        lastRecipe = recipe;

        var outputs = CraftingRecipeMatcher.outputsOf(recipe, input);
        if (outputs == null || outputs.length == 0) {
            playFailSound();
            return 0;
        }

        var inputSlots = getActiveInputSlots(container);
        if (inputSlots.isEmpty()) {
            playFailSound();
            return 0;
        }

        var craftCount = Math.min(maxCrafts, computeMaxCraftRepetitions(container, inputSlots));
        if (craftCount < 1) {
            playFailSound();
            return 0;
        }

        var facing = getOutputFacing();
        craftCount = limitByOutputCapacity(craftCount, outputs, facing);
        if (craftCount < 1) {
            playFailSound();
            return 0;
        }

        ejectResults(outputs, facing, craftCount);
        consumeBatch(container, inputSlots, craftCount);
        playCraftEffects(facing);
        syncRecipePreviewToViewers();
        return craftCount;
    }

    @Override
    public boolean canCraftMore() {
        if (!isCrafterBlock()) {
            return false;
        }

        var container = getContainer();
        if (!areAllEnabledSlotsFilled(container)) {
            return false;
        }

        var input = (CraftingRecipeInput) container.createRecipeInput();
        var recipe = CraftingRecipeMatcher.match(input, lastRecipe);
        if (recipe == null) {
            return false;
        }
        lastRecipe = recipe;

        var outputs = CraftingRecipeMatcher.outputsOf(recipe, input);
        if (outputs == null || outputs.length == 0) {
            return false;
        }

        var inputSlots = getActiveInputSlots(container);
        if (inputSlots.isEmpty()) {
            return false;
        }

        return limitByOutputCapacity(computeMaxCraftRepetitions(container, inputSlots), outputs, getOutputFacing()) > 0;
    }

    @Override
    public void syncRecipePreviewToViewers() {
        var container = getContainer();
        if (container.getViewers().isEmpty()) {
            return;
        }

        ItemStack preview = ItemAirStack.AIR_STACK;
        if (areAllEnabledSlotsFilled(container)) {
            var input = (CraftingRecipeInput) container.createRecipeInput();
            var recipe = CraftingRecipeMatcher.match(input, lastRecipe);
            if (recipe != null) {
                lastRecipe = recipe;
                var outputs = CraftingRecipeMatcher.outputsOf(recipe, input);
                if (outputs != null && outputs.length > 0) {
                    preview = outputs[0].copy(false);
                }
            }
        }

        var finalPreview = preview;
        container.getViewers().forEach((id, viewer) -> viewer.viewCrafterRecipePreview(container, finalPreview));
    }

    @Override
    public void syncSlotMaskToViewers() {
        sendBlockEntityToViewers();
        position.dimension().updateComparatorOutputLevel(position);
    }

    public CrafterContainer getContainer() {
        return containerHolderComponent.getContainer();
    }

    protected boolean isCrafterBlock() {
        return position.dimension().getBlockState(position).getBlockType() == BlockTypes.CRAFTER;
    }

    protected BlockFace getOutputFacing() {
        var orientation = position.dimension().getBlockState(position).getPropertyValue(BlockPropertyTypes.ORIENTATION);
        return getOutputFacing(orientation);
    }

    public static BlockFace getOutputFacing(Orientation orientation) {
        return switch (orientation) {
            case DOWN_EAST, DOWN_NORTH, DOWN_SOUTH, DOWN_WEST -> BlockFace.DOWN;
            case UP_EAST, UP_NORTH, UP_SOUTH, UP_WEST -> BlockFace.UP;
            case EAST_UP -> BlockFace.EAST;
            case WEST_UP -> BlockFace.WEST;
            case NORTH_UP -> BlockFace.NORTH;
            case SOUTH_UP -> BlockFace.SOUTH;
        };
    }

    protected boolean areAllEnabledSlotsFilled(CrafterContainer container) {
        var hasEnabledSlot = false;
        for (int slot = 0; slot < CrafterContainer.SIZE; slot++) {
            if (container.isSlotDisabled(slot)) {
                continue;
            }

            hasEnabledSlot = true;
            if (container.isEmpty(slot)) {
                return false;
            }
        }

        return hasEnabledSlot;
    }

    protected List<Integer> getActiveInputSlots(CrafterContainer container) {
        var slots = new ArrayList<Integer>(CrafterContainer.SIZE);
        for (int slot = 0; slot < CrafterContainer.SIZE; slot++) {
            if (!container.isSlotDisabled(slot) && !container.isEmpty(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    protected int computeMaxCraftRepetitions(CrafterContainer container, List<Integer> inputSlots) {
        var max = Integer.MAX_VALUE;
        for (var slot : inputSlots) {
            var count = container.getItemStack(slot).getCount();
            if (count < 1) {
                return 0;
            }
            max = Math.min(max, count);
        }

        return max == Integer.MAX_VALUE ? 0 : max;
    }

    protected int limitByOutputCapacity(int repetitions, ItemStack[] outputs, BlockFace facing) {
        if (repetitions < 1) {
            return 0;
        }

        var target = findTargetContainer(facing);
        if (target == null) {
            return repetitions;
        }

        var limited = repetitions;
        for (var output : outputs) {
            var perCraft = Math.max(1, output.getCount());
            limited = Math.min(limited, countAddableSpace(target, output) / perCraft);
            if (limited < 1) {
                return 0;
            }
        }

        return limited;
    }

    protected int countAddableSpace(Container target, ItemStack template) {
        var maxStackSize = template.getItemType().getItemData().maxStackSize();
        var space = 0;
        for (var slot : getInsertSlots(target, template)) {
            var stack = target.getItemStack(slot);
            if (stack == ItemAirStack.AIR_STACK) {
                space += maxStackSize;
            } else if (stack.canMerge(template, true)) {
                space += Math.max(0, maxStackSize - stack.getCount());
            }
        }

        return space;
    }

    protected void ejectResults(ItemStack[] outputs, BlockFace facing, int multiplier) {
        var target = findTargetContainer(facing);
        for (var output : outputs) {
            var remaining = output.getCount() * multiplier;
            var maxStackSize = output.getItemType().getItemData().maxStackSize();
            while (remaining > 0) {
                var portion = output.copy(false);
                portion.setCount(Math.min(remaining, maxStackSize));
                remaining -= portion.getCount();

                if (target != null) {
                    insertIntoContainer(target, portion);
                }
                if (portion.getCount() > 0) {
                    dropItem(portion, facing);
                }
            }
        }
    }

    protected void insertIntoContainer(Container target, ItemStack stack) {
        var maxStackSize = stack.getItemType().getItemData().maxStackSize();
        for (var slot : getInsertSlots(target, stack)) {
            if (stack.getCount() <= 0) {
                return;
            }

            var existing = target.getItemStack(slot);
            if (existing == ItemAirStack.AIR_STACK) {
                var placed = stack.copy(false);
                placed.setCount(Math.min(stack.getCount(), maxStackSize));
                target.setItemStack(slot, placed);
                stack.reduceCount(placed.getCount());
                continue;
            }

            if (!existing.canMerge(stack, true)) {
                continue;
            }

            var moved = Math.min(stack.getCount(), maxStackSize - existing.getCount());
            if (moved <= 0) {
                continue;
            }

            existing.increaseCount(moved);
            target.notifySlotChange(slot);
            stack.reduceCount(moved);
        }
    }

    protected int[] getInsertSlots(Container target, ItemStack stack) {
        if (target instanceof SidedContainer sidedContainer) {
            var allowedSlots = sidedContainer.getAllowedInsertSlots(getOutputFacing().opposite(), stack);
            if (allowedSlots != null) {
                return allowedSlots;
            }
        }

        var size = target.getItemStackArray().length;
        var slots = new int[size];
        for (int slot = 0; slot < size; slot++) {
            slots[slot] = slot;
        }
        return slots;
    }

    protected Container findTargetContainer(BlockFace facing) {
        return findContainerAt(new Position3i(facing.offsetPos(position), position.dimension()));
    }

    protected Container findContainerAt(Position3ic pos) {
        var blockEntity = pos.dimension().getBlockEntity(pos);
        if (!(blockEntity instanceof BlockEntityContainerHolderComponent holder)) {
            return null;
        }

        if (holder instanceof BlockEntityChest chest && chest.isPaired()) {
            return chest.getDoubleChestContainer();
        }

        return holder.getContainer();
    }

    protected void dropItem(ItemStack stack, BlockFace facing) {
        var offset = facing.getOffset();
        var dropPos = new Vector3d(
                position.x() + 0.5 + offset.x() * 0.7,
                position.y() + 0.5 + offset.y() * 0.7,
                position.z() + 0.5 + offset.z() * 0.7
        );
        if (facing == BlockFace.UP || facing == BlockFace.DOWN) {
            dropPos.y -= 0.125;
        }

        var random = ThreadLocalRandom.current();
        var motion = new Vector3d(
                offset.x() * 0.2 + (random.nextDouble() - 0.5) * 0.04,
                0.2,
                offset.z() * 0.2 + (random.nextDouble() - 0.5) * 0.04
        );
        position.dimension().dropItem(stack, dropPos, motion);
    }

    protected void consumeBatch(CrafterContainer container, List<Integer> slots, int count) {
        if (count < 1) {
            return;
        }

        for (var slot : slots) {
            var stack = container.getItemStack(slot);
            stack.reduceCount(count);
            if (stack.getCount() <= 0) {
                container.clearSlot(slot);
            } else {
                container.notifySlotChange(slot);
            }
        }
    }

    protected void playCraftEffects(BlockFace facing) {
        var dimension = position.dimension();
        dimension.addSound(MathUtils.center(position), new CustomSound(SoundNames.CRAFTER_CRAFT));
        dimension.addParticle(MathUtils.center(position), new ShootParticle(facing));
    }

    protected void playFailSound() {
        position.dimension().addSound(MathUtils.center(position), new CustomSound(SoundNames.CRAFTER_FAIL));
    }
}
