package org.allaymc.api.entity.ai;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.interfaces.EntityPlayer;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Eklentilerin mob yapay zekasina mudahale ettigi kancalar.
 *
 * <p>Motor kurallari her sunucuda ayni olmali ama bazı kararlar eklentiye aittir: gorunmez
 * (vanish) bir yetkiliyi kimin goremeyecegi ya da hangi mobun gun isiginda yanmayacagi gibi.
 * Kancalar tek bir statik noktada toplanir; varsayilanlar vanilla davranisidir.</p>
 */
public final class MobAiHooks {

    private static volatile BiPredicate<Entity, EntityPlayer> targetFilter = (mob, player) -> true;
    private static volatile Predicate<Entity> sunlightImmunity = mob -> false;

    private MobAiHooks() {
    }

    /**
     * Bir oyuncunun mobun hedefi olup olamayacagini belirleyen suzgeci ayarlar. Suzgec
     * {@code false} donerse mob oyuncuyu hic gormez; gamemode kontrolu bundan once yapilir.
     *
     * @param filter {@code (mob, oyuncu)} cifti hedef olabiliyorsa {@code true} donen suzgec
     */
    public static void setTargetFilter(BiPredicate<Entity, EntityPlayer> filter) {
        targetFilter = filter == null ? (mob, player) -> true : filter;
    }

    /**
     * @param mob mob
     * @param player aday hedef
     * @return oyuncu mobun hedefi olabilirse {@code true}
     */
    public static boolean canTarget(Entity mob, EntityPlayer player) {
        return targetFilter.test(mob, player);
    }

    /**
     * Gun isiginda yanmayacak mobu belirleyen suzgeci ayarlar.
     *
     * @param immunity mob gun isigindan etkilenmiyorsa {@code true} donen suzgec
     */
    public static void setSunlightImmunity(Predicate<Entity> immunity) {
        sunlightImmunity = immunity == null ? mob -> false : immunity;
    }

    /**
     * @param mob mob
     * @return mob gun isiginda yanmiyorsa {@code true}
     */
    public static boolean isSunlightImmune(Entity mob) {
        return sunlightImmunity.test(mob);
    }
}
