package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityCreeperBaseComponent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Creeper'in temel davranisi: carpisma kutusu ve fitil durumu.
 *
 * <p>Fitil saldiri executor'unun icinde degil burada tutuluyor, cunku istemcinin bundan
 * haberdar olmasi gerek: sisme animasyonunu bir varlik bayragi surukluyor ve bayraklar
 * yalnizca varlik durumu yayinlandiginda karsi tarafa gidiyor.</p>
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
