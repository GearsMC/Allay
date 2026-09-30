package org.allaymc.api.container.interfaces;

import org.allaymc.api.container.FakeContainerFactory;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.player.Player;
import org.allaymc.api.trade.TradeOffer;

import java.util.List;

/**
 * Köylü takas arayüzünü açan sahte konteyner.
 * <p>
 * Dünyada blok gerektirmez; arayüz, istemcide görünen bir tüccar varlığına
 * bağlanır ({@link #setTrader(Entity)}). Başlık {@link #setCustomName(String)}
 * ile verilir. İki yuvası vardır: oyuncunun ödeme eşyalarını koyduğu
 * {@link #INGREDIENT_1_SLOT} ve {@link #INGREDIENT_2_SLOT}. Pencere kapanınca
 * yuvalarda kalan eşyalar oyuncuya geri verilir.
 * <p>
 * Takas doğrulaması ve ödeme düşümü sunucuda yapılır. Her açılış için yeni bir
 * konteyner oluştur ({@link FakeContainerFactory#createFakeTradeContainer()}).
 */
public interface FakeTradeContainer extends FakeContainer {
    int INGREDIENT_1_SLOT = 0;
    int INGREDIENT_2_SLOT = 1;

    /**
     * @param trader istemcide takas arayüzünün bağlanacağı varlık; genelde köylü
     */
    void setTrader(Entity trader);

    Entity getTrader();

    /**
     * Teklifleri ayarlar. Liste sırası arayüzdeki sıradır.
     *
     * @param offers en az bir teklif
     */
    void setOffers(List<TradeOffer> offers);

    List<TradeOffer> getOffers();

    /**
     * Başarılı her takastan sonra çağrılır (ürün oyuncuya verilmeden hemen önce).
     *
     * @param listener dinleyici, {@code null} ile kaldırılır
     */
    void setTradeListener(TradeListener listener);

    TradeListener getTradeListener();

    @FunctionalInterface
    interface TradeListener {
        /**
         * @param player     takası yapan oyuncu
         * @param offer      seçilen teklif
         * @param tradeCount bu istekte yapılan takas sayısı
         */
        void onTrade(Player player, TradeOffer offer, int tradeCount);
    }
}
