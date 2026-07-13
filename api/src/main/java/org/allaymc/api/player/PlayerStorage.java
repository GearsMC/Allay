package org.allaymc.api.player;

/**
 * PlayerStorage stores every player's data keyed by the player's xuid. Since the xuid never
 * changes even if the player renames his xbox account, no data is lost after a name change.
 *
 * @author daoge_cmd
 */
public interface PlayerStorage {

    /**
     * Reads the player data for the given xuid.
     *
     * @param xuid The xuid of the player
     * @return The player data for the given xuid
     */
    PlayerData readPlayerData(String xuid);

    /**
     * Reads the player data for the given player.
     *
     * @param player The player
     * @return The player data for the given player
     */
    default PlayerData readPlayerData(Player player) {
        return readPlayerData(player.getXuid());
    }

    /**
     * Saves the player data for the given xuid.
     *
     * @param xuid       The xuid of the player
     * @param playerData The player data to save
     */
    void savePlayerData(String xuid, PlayerData playerData);

    /**
     * Saves the player data for the given player.
     *
     * @param player The player
     */
    default void savePlayerData(Player player) {
        savePlayerData(player.getXuid(), PlayerData.save(player));
    }

    /**
     * Removes the player data for the given xuid.
     *
     * @param xuid The xuid of the player
     * @return {@code true} if the player data was removed, {@code false} otherwise.
     */
    boolean removePlayerData(String xuid);

    /**
     * Checks if the player data exists for the given xuid.
     *
     * @param xuid The xuid of the player
     * @return {@code true} if the player data exists, {@code false} otherwise.
     */
    boolean hasPlayerData(String xuid);

    /**
     * Checks if the player data exists for the given player.
     *
     * @param player The player
     * @return {@code true} if the player data exists, {@code false} otherwise.
     */
    default boolean hasPlayerData(Player player) {
        return hasPlayerData(player.getXuid());
    }
}
