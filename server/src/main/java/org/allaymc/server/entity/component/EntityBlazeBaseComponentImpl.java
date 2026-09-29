package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Blaze'in temel davranisi: carpisma kutusu ve sarj gorunumu.
 *
 * <p>Blaze'in cubuklarinin hizlanip parladigi "ofkeli" poz yalnizca bir ates topu serisini
 * sarj ederken ve savururken gosterilir; hedefi gormek ya da yakinda bir oyuncu bulunmasi
 * tek basina yetmez. Aksi halde blaze dogar dogmaz (yakinda oyuncu varsa) ya da yapay zekasi
 * dondurulmusken bile ofkeli gorunurdu. Durumu {@code FireballAttackExecutor} yazar (paralel
 * yapay zeka tick'inde); yayin sirali varlik tick'inde, yalnizca durum degistigi an yapilir.</p>
 */
public class EntityBlazeBaseComponentImpl extends EntityBaseComponentImpl {

    protected volatile boolean charging;
    protected boolean broadcastCharging;

    public EntityBlazeBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
    }

    /**
     * @return blaze su an bir ates topu serisini sarj ediyor ya da savuruyor mu
     */
    public boolean isCharging() {
        return charging;
    }

    /**
     * @param charging yeni sarj durumu; istemcilere bir sonraki varlik tick'inde yayinlanir
     */
    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    @EventHandler
    protected void onChargeTick(CEntityTickEvent event) {
        var current = charging;
        if (current == broadcastCharging) {
            return;
        }

        broadcastCharging = current;
        broadcastState();
    }
}
