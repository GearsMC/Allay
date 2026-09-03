package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Arinin temel davranisi: yalnizca carpisma kutusu.
 *
 * <p>Olculer vanilla ariyla ayni (0.7 genislik, 0.6 yukseklik); taban bilesenin
 * varsayilani oyuncu kutusudur ve ari onunla gercek boyutunun dort kati bir
 * hedef olurdu.</p>
 */
public class EntityBeeBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityBeeBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.35, 0.0, -0.35, 0.35, 0.6, 0.35);
    }
}
