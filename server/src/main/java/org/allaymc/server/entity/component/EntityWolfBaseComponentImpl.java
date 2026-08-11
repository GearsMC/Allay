package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Wolf base behavior: hitbox only.
 *
 * <p>Only the hostile side of the wolf is implemented — taming, sitting and collar colours are
 * deliberately left out, since wolves currently reach the world through the GearsCore mob block
 * as plain enemies.</p>
 */
public class EntityWolfBaseComponentImpl extends EntityAngerableBaseComponentImpl {

    public EntityWolfBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 0.85, 0.3);
    }
}
