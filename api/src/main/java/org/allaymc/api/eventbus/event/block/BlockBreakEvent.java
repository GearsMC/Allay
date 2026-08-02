package org.allaymc.api.eventbus.event.block;

import lombok.Getter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.eventbus.event.CancellableEvent;
import org.allaymc.api.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Called when a block is broken.
 *
 * @author daoge_cmd
 */
@Getter
@CallerThread(ThreadType.WORLD)
@CallerThread(ThreadType.DIMENSION)
public class BlockBreakEvent extends BlockEvent implements CancellableEvent {
    /**
     * The item used to break the block. Can be {@code null}.
     */
    protected ItemStack usedItem;
    /**
     * The entity breaks the block. Can be {@code null}.
     */
    protected Entity entity;
    /**
     * The item stacks the break will yield, or {@code null} while they are still
     * untouched. Resolved on first access so that a break nobody listens to never
     * pays for the drop calculation.
     */
    protected List<ItemStack> drops;

    public BlockBreakEvent(Block block, ItemStack usedItem, Entity entity) {
        super(block);
        this.usedItem = usedItem;
        this.entity = entity;
    }

    /**
     * Returns the item stacks this break will yield, taking the creative game mode,
     * the required tool and the silk touch enchantment into account.
     * <p>
     * The returned list is mutable and is what actually gets dropped, so it can be
     * added to, filtered or cleared in place - clearing it makes the break yield
     * nothing. An empty list is a legitimate answer for blocks that never drop.
     *
     * @return the mutable drop list
     */
    public List<ItemStack> getDrops() {
        if (drops == null) {
            drops = new ArrayList<>(block.getBehavior().resolveDrops(block, usedItem, entity));
        }
        return drops;
    }

    /**
     * Replaces what this break will yield.
     *
     * @param drops the item stacks to drop, an empty collection to drop nothing
     */
    public void setDrops(Collection<ItemStack> drops) {
        this.drops = new ArrayList<>(drops);
    }

    /**
     * Tells whether the drop list has been touched by a listener.
     * <p>
     * When it has not, the caller can let the block resolve its own drops and keep
     * the exact behaviour it had before drops became overridable.
     *
     * @return {@code true} if a listener read or replaced the drop list
     */
    @ApiStatus.Internal
    public boolean isDropsResolved() {
        return drops != null;
    }
}
