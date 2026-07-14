package org.allaymc.api.scoreboard.scorer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.allaymc.api.player.Player;
import org.allaymc.api.server.Server;

/**
 * PlayerScorer is a scorer that represents a player.
 *
 * @author daoge_cmd
 */
@Getter
@AllArgsConstructor
public final class PlayerScorer implements Scorer {

    // Oyuncu ismini değiştirse bile skorlarının kaybolmaması için xuid ile anahtarlanır
    private final String xuid;

    public PlayerScorer(Player player) {
        this(player.getXuid());
    }

    public Player getPlayer() {
        if (xuid == null) {
            return null;
        }

        return Server.getInstance().getPlayerManager().getPlayerByXuid(xuid);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    @Override
    public ScorerType getScorerType() {
        return ScorerType.PLAYER;
    }

    @Override
    public int hashCode() {
        return xuid != null ? xuid.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayerScorer playerScorer) {
            return xuid.equals(playerScorer.xuid);
        }
        return false;
    }

    @Override
    public String getName() {
        var player = getPlayer();
        if (player != null) {
            return player.getOriginName();
        }

        // Oyuncu çevrimdışıysa bilinen son ismi kimlik indeksinden bulunur
        return Server.getInstance().getPlayerManager().getPlayerIdentityStorage()
                .lookupNameByXuid(xuid)
                .orElse(xuid);
    }

}
