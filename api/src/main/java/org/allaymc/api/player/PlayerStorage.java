package org.allaymc.api.player;

/**
 * PlayerStorage, her oyuncunun verisini oyuncunun xuid'i ile anahtarlayarak saklar. Xuid,
 * oyuncu xbox hesabının ismini değiştirse bile asla değişmediğinden isim değişikliği sonrası
 * hiçbir veri kaybolmaz.
 *
 * @author daoge_cmd
 */
public interface PlayerStorage {

    /**
     * Verilen xuid için oyuncu verisini okur.
     *
     * @param xuid oyuncunun xuid'i
     * @return verilen xuid'e ait oyuncu verisi
     */
    PlayerData readPlayerData(String xuid);

    /**
     * Verilen oyuncu için oyuncu verisini okur.
     *
     * @param player oyuncu
     * @return verilen oyuncuya ait oyuncu verisi
     */
    default PlayerData readPlayerData(Player player) {
        return readPlayerData(player.getXuid());
    }

    /**
     * Verilen xuid için oyuncu verisini kaydeder.
     *
     * @param xuid       oyuncunun xuid'i
     * @param playerData kaydedilecek oyuncu verisi
     */
    void savePlayerData(String xuid, PlayerData playerData);

    /**
     * Verilen oyuncu için oyuncu verisini kaydeder.
     *
     * @param player oyuncu
     */
    default void savePlayerData(Player player) {
        savePlayerData(player.getXuid(), PlayerData.save(player));
    }

    /**
     * Verilen xuid için oyuncu verisini siler.
     *
     * @param xuid oyuncunun xuid'i
     * @return oyuncu verisi silindiyse {@code true}, aksi halde {@code false}.
     */
    boolean removePlayerData(String xuid);

    /**
     * Verilen xuid için oyuncu verisinin var olup olmadığını kontrol eder.
     *
     * @param xuid oyuncunun xuid'i
     * @return oyuncu verisi varsa {@code true}, aksi halde {@code false}.
     */
    boolean hasPlayerData(String xuid);

    /**
     * Verilen oyuncu için oyuncu verisinin var olup olmadığını kontrol eder.
     *
     * @param player oyuncu
     * @return oyuncu verisi varsa {@code true}, aksi halde {@code false}.
     */
    default boolean hasPlayerData(Player player) {
        return hasPlayerData(player.getXuid());
    }
}
