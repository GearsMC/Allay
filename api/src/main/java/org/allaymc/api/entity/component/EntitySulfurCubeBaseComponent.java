package org.allaymc.api.entity.component;

import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.entity.data.SulfurCubeArchetype;

/**
 * Sulfur kupunun boyutunu, emdigi blogu ve fitilini tutan bilesen.
 */
public interface EntitySulfurCubeBaseComponent extends EntityBaseComponent {

    /**
     * @return kupun buyuk olup olmadigi. Yalnizca buyuk kupler blok emebilir ve olunce bolunur.
     */
    boolean isLarge();

    /**
     * Kupun boyutunu ayarlar; can ve carpisma kutusu buna gore yeniden hesaplanir.
     *
     * @param large buyuk yapmak icin {@code true}
     */
    void setLarge(boolean large);

    /**
     * @return kupun icindeki blok; bos ise {@code null}
     */
    BlockState getAbsorbedBlock();

    /**
     * Kupun icindeki blogu ayarlar. Kisiligi de bu blok belirler.
     *
     * @param blockState emilecek blok; cikarmak icin {@code null}
     */
    void setAbsorbedBlock(BlockState blockState);

    /**
     * @return emilen blogun verdigi kisilik; kup bossa {@code null}
     */
    SulfurCubeArchetype getArchetype();

    /**
     * @return kupun su an yerden esya alamayacagi. Blogu cikarildiktan hemen sonra kisa bir sure
     * dogru doner; boylece kup biraktigi blogu aninda geri yutmaz.
     */
    boolean isPickupOnCooldown();

    /**
     * @return fitilin yanip yanmadigi. Yanan bir kupe zarar verilemez, kovaya alinamaz ve blogu
     * makasla cikarilamaz.
     */
    boolean isIgnited();

    /**
     * TNT tasiyan bir kupun fitilini yakar. Baska bir kisilikteki kupte hicbir sey yapmaz.
     *
     * @param fuseTicks fitilin kac tick sonra patlayacagi
     */
    void ignite(int fuseTicks);

    /**
     * @return kupun su an ziplama animasyonunda olup olmadigi
     */
    boolean isJumping();

    /**
     * @return ziplama animasyonunun bitmesine kalan tick; istemciye bu sure gonderilir
     */
    int getJumpDurationTicks();

    /**
     * Ziplama animasyonunu baslatir. Kupun havadaki hareketi fizige ait; bu yalnizca istemcinin
     * hangi animasyonu oynatacagini ve ne kadar sureyle oynatacagini soyler.
     *
     * @param durationTicks animasyonun kac tick surecegi
     */
    void startJump(int durationTicks);
}
