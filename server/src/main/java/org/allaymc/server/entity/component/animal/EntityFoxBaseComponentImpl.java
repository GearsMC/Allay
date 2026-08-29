package org.allaymc.server.entity.component.animal;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Provides breeding interaction and the adult collision box for foxes.
 */
public class EntityFoxBaseComponentImpl extends EntityAnimalBaseComponentImpl {

    public EntityFoxBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 0.7, 0.3);
    }
}
