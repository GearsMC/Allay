package org.allaymc.server.entity.component.aquatic;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Akselotun temel davranisi: carpisma kutusu ve renk varyanti.
 *
 * <p>Vanilla'da bes renk var ve hangisinin dogacagi sansa bakiyor; mavi son derece nadir, cunku
 * dogal olarak hic dogmaz, yalnizca ureme sonucu cikar. Burada da ayni nadirlik korunuyor.</p>
 */
public class EntityAxolotlBaseComponentImpl extends EntityBaseComponentImpl {

    /** Vanilla renk sirasi: pembe, kahverengi, altin, mavi-yesil, mavi. */
    public static final int VARIANT_COUNT = 5;

    /** Mavi varyantin sirasi; digerlerinin aksine neredeyse hic cikmaz. */
    protected static final int BLUE_VARIANT = 4;

    protected final int variant;

    public EntityAxolotlBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
        var rand = ThreadLocalRandom.current();
        // Binde bir mavi, geri kalanda ilk dort renkten biri.
        this.variant = rand.nextInt(1000) == 0 ? BLUE_VARIANT : rand.nextInt(BLUE_VARIANT);
    }

    /**
     * @return akselotun renk varyanti; istemciye metadata ile gonderilir
     */
    public int getVariant() {
        return variant;
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.375, 0.0, -0.375, 0.375, 0.42, 0.375);
    }
}
