package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Gumus balik carpisma kutusu: vanilla 0.4 genislik, 0.3 yukseklik.
 */
public class EntitySilverfishBaseComponentImpl extends EntityBaseComponentImpl {

    public EntitySilverfishBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.2, 0.0, -0.2, 0.2, 0.3, 0.2);
    }
}
