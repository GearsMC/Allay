package org.allaymc.api.entity.component;

/**
 * Fitilinin yanip yanmadigini takip eden creeper varliklari icin ortak bilesen.
 */
public interface EntityCreeperBaseComponent extends EntityBaseComponent {

    /**
     * Bu creeper'in su an patlamak uzere sisip sismedigini kontrol eder.
     *
     * @return fitil yaniyorsa {@code true}
     */
    boolean isSwelling();

    /**
     * Bu creeper'in fitilinin yanip yanmadigini ayarlar. Istemcideki sisme animasyonunu bu
     * suruklerdir.
     *
     * @param swelling fitili yakmak icin {@code true}
     */
    void setSwelling(boolean swelling);
}
