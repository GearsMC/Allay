package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Fantom carpisma kutusu: vanilla 0.9 genislik, 0.5 yukseklik.
 */
public class EntityPhantomBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityPhantomBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.45, 0.0, -0.45, 0.45, 0.5, 0.45);
    }
}
