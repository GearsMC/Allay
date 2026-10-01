package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Orumcegin temel davranisi: carpisma kutusu (vanilla 1.4 x 0.9) ve duvar tirmanma gorunumu.
 *
 * <p>Tirmanma durumunu fizik bileseni yazar ({@link EntitySpiderPhysicsComponentImpl}); yayin
 * sirali varlik tick'inde, yalnizca durum degistigi an yapilir (blaze sarji ile ayni kalip).</p>
 */
public class EntitySpiderBaseComponentImpl extends EntityBaseComponentImpl {

    protected volatile boolean climbing;
    protected boolean broadcastClimbing;

    public EntitySpiderBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.7, 0.0, -0.7, 0.7, 0.9, 0.7);
    }

    /**
     * @return orumcek su an duvara tirmaniyor mu
     */
    public boolean isClimbing() {
        return climbing;
    }

    /**
     * @param climbing yeni durum; istemcilere bir sonraki varlik tick'inde yayinlanir
     */
    public void setClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    @EventHandler
    protected void onClimbTick(CEntityTickEvent event) {
        var current = climbing;
        if (current == broadcastClimbing) {
            return;
        }

        broadcastClimbing = current;
        broadcastState();
    }
}
