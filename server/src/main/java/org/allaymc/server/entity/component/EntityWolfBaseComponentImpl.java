package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Kurdun temel davranisi: yalnizca carpisma kutusu.
 *
 * <p>Kurdun sadece saldirgan tarafi islendi; evcillestirme, oturma ve tasma rengi bilerek
 * disarida birakildi, cunku kurtlar dunyaya GearsCore mob blogundan duz birer dusman olarak
 * geliyor.</p>
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
