package org.allaymc.api.player;

import java.util.Optional;

/**
 * PlayerIdentityStorage maintains a persistent, rename-safe index between players' xuids and
 * their last known names. Player data itself is keyed by xuid (see {@link PlayerStorage}), so
 * this index is what allows name based inputs (e.g. {@code /op <name>}) to be resolved to a
 * xuid without scanning every stored player data entry.
 */
public interface PlayerIdentityStorage {

    /**
     * Remembers the identity of a player. If the player was known before under a different name,
     * the old name mapping will be replaced with the new one, unless the old name has been taken
     * over by another player in the meantime.
     *
     * @param xuid the xuid of the player
     * @param name the current name of the player
     */
    void rememberIdentity(String xuid, String name);

    /**
     * Remembers the identity of the given player.
     *
     * @param player the player
     */
    default void rememberIdentity(Player player) {
        rememberIdentity(player.getXuid(), player.getOriginName());
    }

    /**
     * Looks up a player's xuid by his last known name. The lookup is case-insensitive.
     *
     * @param name the last known name of the player
     * @return the xuid of the player, or empty if the name is unknown
     */
    Optional<String> lookupXuidByName(String name);

    /**
     * Looks up a player's last known name by his xuid.
     *
     * @param xuid the xuid of the player
     * @return the last known name of the player, or empty if the xuid is unknown
     */
    Optional<String> lookupNameByXuid(String xuid);
}
