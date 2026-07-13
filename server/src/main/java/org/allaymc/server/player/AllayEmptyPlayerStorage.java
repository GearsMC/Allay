package org.allaymc.server.player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.allaymc.api.player.PlayerData;

/**
 * @author daoge_cmd
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AllayEmptyPlayerStorage extends AllayPlayerStorage {

    public static final AllayEmptyPlayerStorage INSTANCE = new AllayEmptyPlayerStorage();

    @Override
    public PlayerData readPlayerData(String xuid) {
        return PlayerData.createEmpty();
    }

    @Override
    public void savePlayerData(String xuid, PlayerData playerData) {
        // Do nothing
    }

    @Override
    public boolean removePlayerData(String xuid) {
        return false;
    }

    @Override
    public boolean hasPlayerData(String xuid) {
        return false;
    }
}
