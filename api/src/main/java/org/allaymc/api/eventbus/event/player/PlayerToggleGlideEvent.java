package org.allaymc.api.eventbus.event.player;

import lombok.Getter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.event.CancellableEvent;

/**
 * Called when a player starts or stops gliding with elytra.
 *
 * <p>The event is fired before the state changes, so cancelling it keeps the
 * player in their current gliding state and the client is corrected. This lets
 * a plugin forbid gliding in an area outright instead of switching it off a
 * tick later, which the player sees as a stutter.</p>
 *
 * @author daoge_cmd
 */
@Getter
@CallerThread(ThreadType.WORLD)
public class PlayerToggleGlideEvent extends PlayerEvent implements CancellableEvent {
    /**
     * Whether gliding is enabled.
     */
    protected boolean value;

    public PlayerToggleGlideEvent(EntityPlayer player, boolean value) {
        super(player);
        this.value = value;
    }
}
