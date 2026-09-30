package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Ender ejderhasi icin temel davranis: yalnizca carpisma kutusu.
 *
 * <p>Varsayilan Bedrock {@code ender_dragon} kutusu (13 genislik, 4 yukseklik). Taban
 * bilesenin varsayilani oyuncu kutusu (0,6 x 1,8) oldugu icin ejderha sunucuda insan
 * boyutunda kaliyordu. {@link #setBaseAABB} ile verilen kutu (ornegin kucuk pet
 * ejderhasi) her zaman once gelir.</p>
 */
public class EntityEnderDragonBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityEnderDragonBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        if (customBaseAABB != null) {
            return customBaseAABB;
        }
        return new AABBd(-6.5, 0.0, -6.5, 6.5, 4.0, 6.5);
    }
}
