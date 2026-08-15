package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Cadinin temel davranisi: yalnizca carpisma kutusu.
 *
 * <p>Diger illager'larin aksine cadi elinde bir sey tasimaz — iksirleri yoktan cikarir — bu
 * yuzden ona konteyner bileseni verilmedi.</p>
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
