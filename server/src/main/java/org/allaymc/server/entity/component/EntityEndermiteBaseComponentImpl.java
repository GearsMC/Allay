package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Endermite base behavior: hitbox only.
 *
 * <p><strong>Deliberate deviation:</strong> a vanilla endermite despawns roughly two minutes
 * after spawning. Endermites here come from GearsCore mob blocks and are meant to be fought,
 * so the self-destruct timer is not implemented.</p>
 */
public class EntityEndermiteBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityEndermiteBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.2, 0.0, -0.2, 0.2, 0.3, 0.2);
    }
}
