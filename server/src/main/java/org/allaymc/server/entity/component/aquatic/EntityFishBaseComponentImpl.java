package org.allaymc.server.entity.component.aquatic;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Baliklarin paylastigi temel davranis: yalnizca carpisma kutusu.
 *
 * <p>Dort balik turu de birbirinden sadece boyut, can ve dusurdukleri esyayla ayrildigi icin
 * carpisma kutusu disariya parametre olarak veriliyor ve geri kalan her sey ortak kaliyor.</p>
 */
public class EntityFishBaseComponentImpl extends EntityBaseComponentImpl {

    protected final AABBdc baseAABB;

    /**
     * @param initInfo varlik baslatma bilgisi
     * @param width carpisma kutusu genisligi (blok)
     * @param height carpisma kutusu yuksekligi (blok)
     */
    public EntityFishBaseComponentImpl(EntityInitInfo initInfo, double width, double height) {
        super(initInfo);
        var halfWidth = width / 2;
        this.baseAABB = new AABBd(-halfWidth, 0.0, -halfWidth, halfWidth, height, halfWidth);
    }

    @Override
    public AABBdc getBaseAABB() {
        return baseAABB;
    }
}
