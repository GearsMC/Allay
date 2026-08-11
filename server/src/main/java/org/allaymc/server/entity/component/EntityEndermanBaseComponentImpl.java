package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Enderman base behavior: hitbox only.
 *
 * <p>The enderman is by far the tallest mob implemented here; the hitbox has to be set explicitly
 * or it would inherit the 1.8-block default and let players hit thin air above its head.</p>
 */
public class EntityEndermanBaseComponentImpl extends EntityAngerableBaseComponentImpl {

    public EntityEndermanBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 2.9, 0.3);
    }
}
