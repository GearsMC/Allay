package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Enderman'in temel davranisi: yalnizca carpisma kutusu.
 *
 * <p>Enderman buradaki en uzun boylu mob; carpisma kutusu acikca verilmezse 1.8 bloklik
 * varsayilani devralir ve oyuncular kafasinin hizasinda bosluga vurur.</p>
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
