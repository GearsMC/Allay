package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.MobAiHooks;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.joml.Vector3dc;

/**
 * Utility class with static helper methods for controlling entity movement
 * and look targets through memory storage.
 *
 * @author daoge_cmd
 */
public final class EntityControlHelper {

    private EntityControlHelper() {
    }

    public static void setRouteTarget(EntityIntelligent entity, Vector3dc target) {
        entity.setMoveTarget(target);
        entity.getBehaviorGroup().setRouteUpdateRequired(true);
    }

    public static void setLookTarget(EntityIntelligent entity, Vector3dc target) {
        entity.setLookTarget(target);
    }

    public static void removeRouteTarget(EntityIntelligent entity) {
        entity.getMemoryStorage().clear(MemoryTypes.MOVE_TARGET);
        entity.setMoveDirectionStart(null);
        entity.setMoveDirectionEnd(null);
    }

    public static void removeLookTarget(EntityIntelligent entity) {
        entity.getMemoryStorage().clear(MemoryTypes.LOOK_TARGET);
    }

    /**
     * Eklenti kancasi (gorunmez yetkili gibi) bu hedefi mobdan gizliyor mu.
     *
     * @param mob saldiran mob
     * @param target hedef
     * @return hedef gecerliyse {@code true}; yalnizca oyuncular suzulur
     */
    public static boolean allowsTarget(Entity mob, Entity target) {
        return !(target instanceof EntityPlayer player) || MobAiHooks.canTarget(mob, player);
    }

    /**
     * Oyuncunun bakisi verilen noktaya yonelik mi (bakis vektoru ile goz-nokta dogrusu arasindaki
     * kosinus esigin ustunde mi).
     *
     * @param player bakan oyuncu
     * @param x nokta x
     * @param y nokta y
     * @param z nokta z
     * @param dotThreshold gerekli kosinus (1 = tam karsisi)
     * @return bakiyorsa {@code true}
     */
    public static boolean isLookingAt(EntityPlayer player, double x, double y, double z, double dotThreshold) {
        var loc = player.getLocation();
        double ex = x - loc.x();
        double ey = y - (loc.y() + player.getEyeHeight());
        double ez = z - loc.z();
        double length = Math.sqrt(ex * ex + ey * ey + ez * ez);
        if (length < 0.0001) {
            return true;
        }
        var look = org.allaymc.api.math.MathUtils.getDirectionVector(loc);
        return (look.x() * ex + look.y() * ey + look.z() * ez) / length >= dotThreshold;
    }
}
