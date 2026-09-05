package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Kurbaga icin temel davranis: yalnizca carpisma kutusu.
 *
 * <p>Olculer vanilla ile ayni (0.5 genislik, 0.5 yukseklik); taban
 * bilesenin varsayilani oyuncu kutusudur ve mob onunla gercek boyutundan cok
 * daha buyuk bir hedef olurdu.</p>
 */
public class EntityFrogBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityFrogBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.25, 0.0, -0.25, 0.25, 0.5, 0.25);
    }
}
