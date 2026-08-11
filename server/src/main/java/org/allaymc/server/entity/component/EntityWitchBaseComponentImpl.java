package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Witch base behavior: hitbox only.
 *
 * <p>Unlike the other illagers the witch carries nothing in its hand — it pulls potions out of thin
 * air — so it gets no container holder.</p>
 */
public class EntityWitchBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityWitchBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 1.95, 0.3);
    }
}
