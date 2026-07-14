package org.allaymc.api.player;

import java.util.Optional;

/**
 * PlayerIdentityStorage, oyuncuların xuid'leri ile bilinen son isimleri arasında kalıcı ve
 * isim değişikliğine dayanıklı bir indeks tutar. Oyuncu verisinin kendisi xuid ile anahtarlanır
 * (bkz. {@link PlayerStorage}); bu indeks sayesinde isim alan girdiler (örn. {@code /op <isim>})
 * kayıtlı tüm oyuncu verilerini taramadan xuid'e çözümlenebilir.
 *
 * @author Clexa
 */
public interface PlayerIdentityStorage {

    /**
     * Oyuncunun kimliğini kaydeder. Oyuncu daha önce farklı bir isimle biliniyorsa eski isim
     * eşlemesi yenisiyle değiştirilir; ancak eski isim bu sırada başka bir oyuncu tarafından
     * alınmışsa ona dokunulmaz.
     *
     * @param xuid oyuncunun xuid'i
     * @param name oyuncunun güncel ismi
     */
    void rememberIdentity(String xuid, String name);

    /**
     * Verilen oyuncunun kimliğini kaydeder.
     *
     * @param player oyuncu
     */
    default void rememberIdentity(Player player) {
        rememberIdentity(player.getXuid(), player.getOriginName());
    }

    /**
     * Bilinen son ismine göre oyuncunun xuid'ini bulur. Arama büyük/küçük harf duyarsızdır.
     *
     * @param name oyuncunun bilinen son ismi
     * @return oyuncunun xuid'i, isim bilinmiyorsa boş
     */
    Optional<String> lookupXuidByName(String name);

    /**
     * Xuid'ine göre oyuncunun bilinen son ismini bulur.
     *
     * @param xuid oyuncunun xuid'i
     * @return oyuncunun bilinen son ismi, xuid bilinmiyorsa boş
     */
    Optional<String> lookupNameByXuid(String xuid);
}
