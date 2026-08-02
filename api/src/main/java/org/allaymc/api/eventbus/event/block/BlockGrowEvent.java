package org.allaymc.api.eventbus.event.block;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.eventbus.event.CancellableEvent;

/**
 * Called when crops grow.
 *
 * @author daoge_cmd
 */
@Getter
@CallerThread(ThreadType.DIMENSION)
public class BlockGrowEvent extends BlockEvent implements CancellableEvent {
    /**
     * The new block state after the growth.
     * <p>
     * Listeners may replace it to alter the outcome of the growth, for example to
     * push a crop one extra stage forward. Every caller in the engine reads the
     * state back from the event once it has been called, so a replacement is
     * always honoured. Cancelling the event skips the growth entirely.
     */
    @Setter
    protected BlockState newBlockState;

    public BlockGrowEvent(Block block, BlockState newBlockState) {
        super(block);
        this.newBlockState = newBlockState;
    }
}
