package org.allaymc.api.trade;

import org.allaymc.api.item.ItemStack;

import java.util.Objects;

/**
 * Köylü takas penceresinde gösterilen tek bir takas teklifi.
 * <p>
 * Oyuncu {@link #getBuyA()} (ve varsa {@link #getBuyB()}) eşyalarını vererek
 * {@link #getSell()} eşyasını alır. Aynı istekte birden çok takas yapılabilir;
 * bu durumda fiyat ve ürün takas sayısıyla çarpılır.
 * <p>
 * Kullanım sayısı teklif nesnesinde tutulur. Aynı teklif listesini birden çok
 * pencerede paylaşırsan stok da paylaşılır.
 */
public final class TradeOffer {
    /**
     * Stok belirtilmediğinde kullanılan üst sınır; pratikte sınırsız.
     */
    public static final int DEFAULT_MAX_USES = 999999;

    private final ItemStack buyA;
    private final ItemStack buyB;
    private final ItemStack sell;
    private final String recipeId;
    private final int maxUses;
    private int uses;

    /**
     * @param buyA     ilk ödeme eşyası, boş olamaz
     * @param buyB     ikinci ödeme eşyası, yoksa {@code null}
     * @param sell     verilen ürün, boş olamaz
     * @param recipeId teklifin kimliği (istemciye gider), boş olabilir
     * @param maxUses  en fazla kaç kez takas yapılabileceği
     */
    public TradeOffer(ItemStack buyA, ItemStack buyB, ItemStack sell, String recipeId, int maxUses) {
        requireStack(buyA, "buyA");
        if (buyB != null) {
            requireStack(buyB, "buyB");
        }
        requireStack(sell, "sell");
        this.buyA = buyA.copy(false);
        this.buyB = buyB == null ? null : buyB.copy(false);
        this.sell = sell.copy(false);
        this.recipeId = recipeId == null ? "" : recipeId;
        this.maxUses = Math.max(1, maxUses);
    }

    /**
     * Tek para birimiyle fiyatlanan teklif oluşturur. Fiyat bir yığına
     * sığmazsa kalan kısım ikinci ödeme eşyasına bölünür (PMMP
     * {@code MerchantTradeOffer::create} ile aynı davranış).
     *
     * @param recipeId teklifin kimliği
     * @param reward   verilen ürün
     * @param currency para birimi eşyası (miktarı dikkate alınmaz)
     * @param price    toplam fiyat
     * @param stock    en fazla kullanım, {@code null} ise sınırsız
     * @return yeni teklif
     * @throws IllegalArgumentException fiyat iki yığına sığmıyorsa
     */
    public static TradeOffer ofCurrency(String recipeId, ItemStack reward, ItemStack currency, int price, Integer stock) {
        requireStack(currency, "currency");
        int maxStack = Math.max(1, currency.getItemType().getItemData().maxStackSize());
        int remaining = Math.max(1, price);
        var buyA = currency.copy(false);
        buyA.setCount(Math.min(maxStack, remaining));
        remaining -= buyA.getCount();
        ItemStack buyB = null;
        if (remaining > 0) {
            if (remaining > maxStack) {
                throw new IllegalArgumentException("Takas fiyatı iki para yığınını aşamaz");
            }
            buyB = currency.copy(false);
            buyB.setCount(remaining);
        }
        return new TradeOffer(buyA, buyB, reward, recipeId, stock == null ? DEFAULT_MAX_USES : stock);
    }

    public ItemStack getBuyA() {
        return buyA.copy(false);
    }

    /**
     * @return ikinci ödeme eşyası, yoksa {@code null}
     */
    public ItemStack getBuyB() {
        return buyB == null ? null : buyB.copy(false);
    }

    public ItemStack getSell() {
        return sell.copy(false);
    }

    public String getRecipeId() {
        return recipeId;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public synchronized int getUses() {
        return uses;
    }

    /**
     * @param tradeCount yapılmak istenen takas sayısı
     * @return stok bu kadar takasa yetiyorsa {@code true}
     */
    public synchronized boolean hasUsesLeft(int tradeCount) {
        return uses + Math.max(1, tradeCount) <= maxUses;
    }

    /**
     * Kullanım sayısını artırır. Sunucu takası tamamladığında çağırır.
     *
     * @param tradeCount tamamlanan takas sayısı
     */
    public synchronized void addUses(int tradeCount) {
        uses += Math.max(1, tradeCount);
    }

    private static void requireStack(ItemStack stack, String name) {
        Objects.requireNonNull(stack, name);
        if (stack.isEmptyOrAir()) {
            throw new IllegalArgumentException("Takas eşyası boş olamaz: " + name);
        }
        if (stack.getCount() > stack.getItemType().getItemData().maxStackSize()) {
            throw new IllegalArgumentException("Takas eşyası yığın sınırını aşıyor: " + name);
        }
    }
}
