package org.allaymc.server.entity.component.sulfurcube;

import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.EntityPhysicsComponentImpl;

/**
 * Sulfur kupunun fizigi: emdigi bloga gore sekme, surtunme, hava direnci ve geri tepme.
 *
 * <p>Kup vurulunca firlatilip bir top gibi sekiyor ve nasil sekecegine emdigi blogun verdigi
 * kisilik karar veriyor. Butun degerler tek bir yerden, {@link SulfurCubeArchetype}'tan geliyor;
 * burasi yalnizca onlari motorun bekledigi parametrelere ceviriyor.</p>
 *
 * <p>Blok tasimayan bir kup sade davranir: sekmez, normal surtunme ve yercekimiyle hareket
 * eder.</p>
 */
public class EntitySulfurCubePhysicsComponentImpl extends EntityPhysicsComponentImpl {

    /** Altinda sekmenin kapatildigi dusus hizi; yoksa kup yerde titrer durur. */
    protected static final double MIN_BOUNCE_SPEED = 0.08;

    @Dependency
    protected EntitySulfurCubeBaseComponent cubeBaseComponent;

    /**
     * Carpismadan onceki dusus hizini saklar.
     *
     * <p>Sekmek icin varligin yere <em>hangi hizla</em> carptigini bilmek gerekiyor, ama motor
     * carpisma sirasinda kalan hareketi dondurdugu icin yere degdigi anda dikey hiz sifirlaniyor.
     * Yani {@link #afterApplyMotion()} calistiginda dusus hizi coktan kaybolmus oluyor; buraya
     * yazilmasinin sebebi bu.</p>
     */
    protected double lastVerticalSpeed;

    @Override
    public double getStepHeight() {
        // Kup ziplayarak ilerliyor; tek bloklik basamaklari asabilmesi icin yeterli pay.
        return 0.6;
    }

    /**
     * Sivi uzerinde duran kisiliklerin yercekimini yenmek icin varsayilan kaldirma kuvvetine
     * uygulanan carpan.
     *
     * <p>Resmi tanim yuzen kupte yercekimini tumden kapatiyor ({@code apply_gravity: false}).
     * Motorda boyle bir anahtar yok, o yuzden ayni sonuca kaldirma kuvvetini yercekimine
     * yetisecek kadar buyuterek varildi: kup batmiyor.</p>
     */
    protected static final double BUOYANT_MULTIPLIER = 2;

    @Override
    public double getDragFactorInAir() {
        // Resmi deger bir carpan, mutlak direnc degil; motorun varsayilaniyla carpilmasi sart.
        // Dogrudan kullanilirsa plaj topunun 1.8'i "her tikte hizin tamami gitsin" demeye gelir ve
        // kup havada asili kalir.
        return super.getDragFactorInAir() * modifier(true);
    }

    @Override
    public double getDragFactorOnGround() {
        // Ayni sekilde carpan: buzun 0.05'i varsayilan 0.09'u 0.0045'e indirir ve kup kayar.
        return super.getDragFactorOnGround() * modifier(false);
    }

    /**
     * @param air hava direnci carpani icin {@code true}, yer surtunmesi icin {@code false}
     * @return kisiligin carpani; blok tasimayan kup icin {@code 1} (degisiklik yok)
     */
    protected double modifier(boolean air) {
        var archetype = cubeBaseComponent.getArchetype();
        if (archetype == null) {
            return 1;
        }

        return air ? archetype.getAirDragModifier() : archetype.getFrictionModifier();
    }

    @Override
    public double getWaterBuoyancy() {
        var archetype = cubeBaseComponent.getArchetype();
        return archetype != null && archetype.isBuoyant()
                ? super.getWaterBuoyancy() * BUOYANT_MULTIPLIER
                : super.getWaterBuoyancy();
    }

    @Override
    public double getLavaBuoyancy() {
        // Resmi tanimda yuzen kisiligin sivi listesi lavi da kapsiyor.
        var archetype = cubeBaseComponent.getArchetype();
        return archetype != null && archetype.isBuoyant()
                ? super.getLavaBuoyancy() * BUOYANT_MULTIPLIER
                : super.getLavaBuoyancy();
    }

    @Override
    public float getKnockbackResistance() {
        var archetype = cubeBaseComponent.getArchetype();
        return archetype == null ? super.getKnockbackResistance() : archetype.getClampedKnockbackResistance();
    }

    @Override
    public boolean applyMotion() {
        lastVerticalSpeed = getMotion().y();
        return super.applyMotion();
    }

    @Override
    public void afterApplyMotion() {
        super.afterApplyMotion();
        bounce();
    }

    /**
     * Yere carpan kupu geri zipatir.
     *
     * <p>Sekme yalnizca kup blok tasirken gecerli: bos bir kup top gibi davranmaz. Cok kucuk
     * dusus hizlarinda sekme kapatiliyor, yoksa kup yerde sonsuza kadar titrer.</p>
     */
    protected void bounce() {
        var archetype = cubeBaseComponent.getArchetype();
        if (archetype == null || archetype.getBounciness() <= 0 || !isOnGround()) {
            return;
        }

        if (lastVerticalSpeed >= -MIN_BOUNCE_SPEED) {
            return;
        }

        var motion = getMotion();
        setMotion(motion.x(), -lastVerticalSpeed * archetype.getBounciness(), motion.z());
        lastVerticalSpeed = 0;
    }
}
