package org.allaymc.api.entity.component;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.item.ItemStack;

/**
 * Shared component for fishing hook entities, covering hook state, catch state, and reeling behavior.
 *
 * @author daoge_cmd
 */
public interface EntityFishingHookBaseComponent extends EntityComponent {
    /**
     * The fishing hook state.
     */
    enum FishingState {
        /**
         * The hook is in the air, flying towards water.
         */
        FLYING,
        /**
         * The hook is in water, waiting for a fish.
         */
        WAITING,
        /**
         * A fish is being attracted to the hook.
         */
        ATTRACTING,
        /**
         * A fish has bitten the hook.
         */
        CAUGHT
    }

    /**
     * Gets the entity that has been hooked by this fishing hook.
     *
     * @return the hooked entity, or {@code null} if none
     */
    Entity getHookedEntity();

    /**
     * Sets the entity that has been hooked by this fishing hook.
     *
     * @param entity the entity to hook, can be {@code null} to clear
     */
    void setHookedEntity(Entity entity);

    /**
     * Checks if this fishing hook has hooked an entity.
     *
     * @return {@code true} if an entity is hooked
     */
    default boolean hasHookedEntity() {
        return getHookedEntity() != null;
    }

    /**
     * Gets the fishing rod item stack associated with this hook.
     *
     * @return the fishing rod, or {@code null} if not set
     */
    ItemStack getFishingRod();

    /**
     * Sets the fishing rod item stack associated with this hook.
     *
     * @param fishingRod the fishing rod, can be {@code null}
     */
    void setFishingRod(ItemStack fishingRod);

    /**
     * Gets the current fishing state.
     *
     * @return the fishing state
     */
    FishingState getFishingState();

    /**
     * Sets the current fishing state.
     *
     * @param state the fishing state
     */
    void setFishingState(FishingState state);

    /**
     * Checks if a fish has bitten the hook and is ready to be reeled in.
     *
     * @return {@code true} if a fish has been caught
     */
    default boolean isCaught() {
        return getFishingState() == FishingState.CAUGHT;
    }

    /**
     * Reels in the fishing line, potentially catching a fish or pulling a hooked entity.
     * This method handles loot generation and experience dropping.
     */
    void reelLine();

    /**
     * Checks if the fishing hook is in open water.
     * Open water is required for catching treasure items.
     *
     * @return {@code true} if in open water
     */
    boolean isInOpenWater();

    /**
     * GearsMC fork: bu kancaya ozel bekleme suresi araligi belirler.
     *
     * <p>Eklenti oltayi kendi seviyelerine gore hizlandirmak isteyebilir. Vanilla
     * araligi (100-600 tik) sabit oldugu icin degeri disaridan verecek bir yol yoktu;
     * ham paket ya da yansitma yerine niyeti tarif eden bu metot eklendi. Yem (Lure)
     * indirimi ile gokyuzu/yagmur duzeltmeleri bu aralik uzerinde de calismaya devam
     * eder, yani yalnizca taban aralik degisir.</p>
     *
     * @param minTicks en kisa bekleme; sifir ya da negatif verilirse vanilla araliga donulur
     * @param maxTicks en uzun bekleme
     */
    void setWaitTimeRange(int minTicks, int maxTicks);

    /**
     * @return ozel bekleme araliginin alt siniri; ayarli degilse {@code 0}
     */
    int getWaitTimeMin();

    /**
     * @return ozel bekleme araliginin ust siniri; ayarli degilse {@code 0}
     */
    int getWaitTimeMax();

    /**
     * GearsMC fork: balik isirdiktan sonra oyuncunun oltayi cekmesi icin taninan sureyi
     * belirler.
     *
     * <p>Vanilla bu sureyi 10 tike sabitler. Eklenti olta seviyesine gore daha genis bir
     * pencere vermek isteyebilir.</p>
     *
     * @param ticks taninan sure; sifir ya da negatif verilirse vanilla 10 tike donulur
     */
    void setCaughtWindowTicks(int ticks);

    /**
     * @return isirma penceresinin tik cinsinden uzunlugu
     */
    int getCaughtWindowTicks();

    /**
     * GearsMC fork: isirma penceresinden geriye kalan tik sayisi.
     *
     * <p>"Mukemmel cekis" gibi zamanlama odullerini olcmek icin gerekir: gecen sure
     * {@link #getCaughtWindowTicks()} eksi bu deger olarak hesaplanir. Durum
     * {@link FishingState#CAUGHT} degilse {@code 0} doner.</p>
     *
     * @return kalan tik sayisi
     */
    int getRemainingCaughtTicks();
}
