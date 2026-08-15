package org.allaymc.server.entity.ai.controller;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.ai.controller.Controller;
import org.allaymc.api.entity.interfaces.EntityIntelligent;

/**
 * Su icindeki hareket kontrolcusu; {@link FlyController}'in sudaki karsiligi.
 *
 * <p>Ucus kontrolcusundan tek farki, hedefe yonelmeden once suda olup olmadigina bakmasi. Karaya
 * vurmus bir balik yuzemez: orada kontrolu birakir ve varlik fizigin insafina, yani cirpinmaya
 * kalir. Bu olmadan kiyiya atilan bir balik havada yuzmeye devam ederdi.</p>
 */
public class SwimController implements Controller {

    /**
     * Bunun uzerindeki kare-hiz carpaninda dis kaynakli hareket (geri tepme, akinti) ezilmez.
     * {@link WalkController}'in kullandigi esigin aynisi.
     */
    protected static final double EXTERNAL_MOTION_THRESHOLD = 0.4756;

    @Override
    public boolean control(EntityIntelligent entity) {
        if (!isInWater(entity)) {
            return false;
        }

        if (!entity.hasMoveDirection()) {
            return false;
        }

        var end = entity.getMoveDirectionEnd();
        if (end == null) {
            return false;
        }

        var motion = entity.getMotion();
        float speed = entity.getMovementSpeed();
        if (motion.lengthSquared() > speed * speed * EXTERNAL_MOTION_THRESHOLD) {
            return false;
        }

        var loc = entity.getLocation();
        double dx = end.x() - loc.x();
        double dy = end.y() - loc.y();
        double dz = end.z() - loc.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.01) {
            return false;
        }

        double factor = Math.min(speed, distance) / distance;
        entity.addMotion(
                dx * factor - motion.x(),
                dy * factor - motion.y(),
                dz * factor - motion.z()
        );

        return true;
    }

    protected boolean isInWater(EntityIntelligent entity) {
        var loc = entity.getLocation();
        var blockState = entity.getDimension().getBlockState(
                (int) Math.floor(loc.x()),
                (int) Math.floor(loc.y()),
                (int) Math.floor(loc.z())
        );
        return blockState.getBlockType().hasBlockTag(BlockTags.WATER);
    }
}
