package org.allaymc.server.entity.ai.controller;

import org.allaymc.api.entity.ai.controller.Controller;
import org.allaymc.api.entity.interfaces.EntityIntelligent;

/**
 * Serbest ucus hareket kontrolcusu; {@link WalkController}'in havadaki karsiligi.
 *
 * <p>Yurume kontrolcusu yalnizca yatay hareketi surukleyip engellerin ustunden ziplatirken, bu
 * kontrolcu hedef noktaya uc eksende birden yonelir. Uc boyutlu rota ureten bir yol bulucuyla ve
 * yercekimi uygulamayan bir fizikle birlikte kullanilmak uzere tasarlandi — aksi halde varlik
 * ara noktalar arasinda asagi duserdi.</p>
 */
public class FlyController implements Controller {

    /**
     * Bunun uzerindeki kare-hiz carpaninda dis kaynakli hareket (geri tepme, patlama) ezilmez,
     * kendi haline birakilir. {@link WalkController}'in kullandigi esigin aynisi.
     */
    protected static final double EXTERNAL_MOTION_THRESHOLD = 0.4756;

    @Override
    public boolean control(EntityIntelligent entity) {
        if (!entity.hasMoveDirection()) {
            return false;
        }

        var end = entity.getMoveDirectionEnd();
        if (end == null) {
            return false;
        }

        var motion = entity.getMotion();
        float speed = entity.getMovementSpeed();
        // Geri tepmeye karsi koymak yerine etkisini gostermesine izin ver.
        if (motion.lengthSquared() > speed * speed * EXTERNAL_MOTION_THRESHOLD) {
            return false;
        }

        var loc = entity.getLocation();
        double dx = end.x() - loc.x();
        double dy = end.y() - loc.y();
        double dz = end.z() - loc.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Zaten varildi — updateRoute() bir sonraki tick'te siradaki ara noktaya gecer.
        if (distance < 0.01) {
            return false;
        }

        // Ara noktaya yaklasinca kirp ki varlik noktayi asip salinmak yerine yerine otursun.
        double factor = Math.min(speed, distance) / distance;
        entity.addMotion(
                dx * factor - motion.x(),
                dy * factor - motion.y(),
                dz * factor - motion.z()
        );

        return true;
    }
}
