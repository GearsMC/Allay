package org.allaymc.api.entity.component;

import org.allaymc.api.entity.data.WeaponStance;

/**
 * Saldirmadan once silahini gozle gorulur sekilde hazirlayan moblar icin ortak bilesen.
 *
 * <p>Durus, yalnizca istemciye ulasmak icin var olan sunucu verisidir: yay/arbalet
 * animasyonlarindan hangisinin oynayacagina karar verir. Uygulamalar deger degistiginde varlik
 * durumunu yayinlar.</p>
 */
public interface EntityWeaponStanceComponent extends EntityBaseComponent {

    /**
     * @return mobun su an icinde bulundugu silah kullanma asamasi
     */
    WeaponStance getWeaponStance();

    /**
     * Silah kullanma asamasini ayarlar. Zaten sahip oldugu degeri vermek hicbir sey yapmaz.
     *
     * @param stance yeni durus
     */
    void setWeaponStance(WeaponStance stance);

    /**
     * @return mobun su an saldirmak istedigi bir sey olup olmadigi. Saldirgan duruşu surukler,
     * ve illager'larda silahi kaldirmalarini saglayan sey de budur.
     */
    boolean isAggressive();

    /**
     * Mobun bir hedefin pesinde olup olmadigini ayarlar.
     *
     * @param aggressive mobun hedefi varsa {@code true}
     */
    void setAggressive(boolean aggressive);
}
