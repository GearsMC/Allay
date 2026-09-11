package org.allaymc.api.command;

import org.allaymc.api.player.Player;

@FunctionalInterface
public interface CommandVisibility {

    boolean isVisible(Command command, Player player);
}
