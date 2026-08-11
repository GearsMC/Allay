package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityCreeperBaseComponent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Creeper base behavior: hitbox and fuse state.
 *
 * <p>The fuse lives here rather than inside the attack executor because the client has to be told
 * about it — the swelling animation is driven by an entity flag, and flags only travel with an
 * entity state broadcast.</p>
 */
public class EntityCreeperBaseComponentImpl extends EntityBaseComponentImpl implements EntityCreeperBaseComponent {

    protected boolean swelling;

    public EntityCreeperBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 1.7, 0.3);
    }

    @Override
    public boolean isSwelling() {
        return swelling;
    }

    @Override
    public void setSwelling(boolean swelling) {
        if (this.swelling == swelling) {
            return;
        }

        this.swelling = swelling;
        broadcastState();
    }
}
