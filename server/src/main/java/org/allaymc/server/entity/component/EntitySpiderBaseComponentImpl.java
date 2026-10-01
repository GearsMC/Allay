package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Orumcegin carpisma kutusu: vanilla 1.4 genislik, 0.9 yukseklik.
 */
public class EntitySpiderBaseComponentImpl extends EntityBaseComponentImpl {

    public EntitySpiderBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.7, 0.0, -0.7, 0.7, 0.9, 0.7);
    }
}
