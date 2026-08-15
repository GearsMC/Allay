package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Blaze'in temel davranisi: carpisma kutusu ve ofkeli gorunum.
 *
 * <p>Ofkelenebilir temel bileseni genisletir, boylece blaze bir hedefe kilitlendiginde istemci
 * haberdar olur: cubuklarinin hizlanip parlamasini saglayan sey budur, vanilla'nin ates topu
 * serisinden once verdigi isaretin aynisi.</p>
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
