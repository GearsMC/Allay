package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Allay icin temel davranis: yalnizca carpisma kutusu.
 *
 * <p>Olculer vanilla ile ayni (0.35 genislik, 0.6 yukseklik); taban
 * bilesenin varsayilani oyuncu kutusudur ve mob onunla gercek boyutundan cok
 * daha buyuk bir hedef olurdu.</p>
 */
public class EntityAllayBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityAllayBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.175, 0.0, -0.175, 0.175, 0.6, 0.175);
    }
}
