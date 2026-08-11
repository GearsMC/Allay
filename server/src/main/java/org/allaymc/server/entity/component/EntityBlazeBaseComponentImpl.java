package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Blaze base behavior: hitbox and the angry/charged look.
 *
 * <p>Extends the angerable base so the client is told when the blaze has locked on — that is what
 * makes its rods spin up and flare, the same cue vanilla gives before a fireball burst.</p>
 */
public class EntityBlazeBaseComponentImpl extends EntityAngerableBaseComponentImpl {

    public EntityBlazeBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
    }
}
