package org.allaymc.api.entity.data;

/**
 * Bir mobun menzilli silah kullanma dongusunun neresinde oldugu.
 *
 * <p>Cekme ve tutma animasyonlarini istemci kendi oynatir, ama yalnizca mobun hangi asamada
 * oldugu kendisine soylendiginde; bu olmadan iskelet yayini yanina indirmis halde dururken
 * icinden oklar cikar.</p>
 */
public enum WeaponStance {
    /**
     * Silah kullanilmiyor. Kollar asagida.
     */
    IDLE,
    /**
     * Yay geriliyor ya da arbalet kuruluyor. Istemci cekme animasyonunu oynatir.
     */
    CHARGING,
    /**
     * Silah tam gerilmis ya da dolu, hedefe tutulmus ve atmak uzere.
     */
    READY
}
