package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Demir golemin temel davranışı: yalnızca çarpışma kutusu.
 *
 * <p>Ölçüler vanilla golemle aynı (1.4 genişlik, 2.7 yükseklik); taban bileşenin varsayılanı
 * oyuncu kutusudur ve golem gövdesinin yarısından vurulamazdı.</p>
 */
public class EntityIronGolemBaseComponentImpl extends EntityBaseComponentImpl {

    public EntityIronGolemBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.7, 0.0, -0.7, 0.7, 2.7, 0.7);
    }
}
