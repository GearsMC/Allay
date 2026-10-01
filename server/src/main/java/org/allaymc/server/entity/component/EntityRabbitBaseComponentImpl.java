package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Tavsan carpisma kutusu: vanilla 0.4 genislik, 0.5 yukseklik.
 */
public class EntityRabbitBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityRabbitBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.2, 0.0, -0.2, 0.2, 0.5, 0.2);
    }
}
