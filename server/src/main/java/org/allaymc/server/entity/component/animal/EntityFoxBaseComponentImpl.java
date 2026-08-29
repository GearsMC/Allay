package org.allaymc.server.entity.component.animal;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityFoxBaseComponent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Provides breeding interaction and the adult collision box for foxes.
 */
public class EntityFoxBaseComponentImpl extends EntityAnimalBaseComponentImpl implements EntityFoxBaseComponent {

    protected boolean sleeping;

    public EntityFoxBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 0.7, 0.3);
    }

    @Override
    public boolean isSleeping() {
        return sleeping;
    }

    @Override
    public void setSleeping(boolean sleeping) {
        if (this.sleeping == sleeping) {
            return;
        }

        this.sleeping = sleeping;
        broadcastState();
    }
}
