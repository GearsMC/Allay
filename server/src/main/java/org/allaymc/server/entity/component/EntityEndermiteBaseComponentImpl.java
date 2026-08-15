package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Endermite'in temel davranisi: yalnizca carpisma kutusu.
 *
 * <p><strong>Bilincli sapma:</strong> vanilla endermite dogduktan yaklasik iki dakika sonra
 * kendini yok eder. Buradaki endermite'lar GearsCore mob bloklarindan cikiyor ve dovusulmek
 * icin varlar, bu yuzden kendini yok etme sayaci eklenmedi.</p>
 */
public class EntityEndermiteBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityEndermiteBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.2, 0.0, -0.2, 0.2, 0.3, 0.2);
    }
}
