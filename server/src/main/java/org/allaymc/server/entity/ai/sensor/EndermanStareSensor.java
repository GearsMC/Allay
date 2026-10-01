package org.allaymc.server.entity.ai.sensor;

import org.allaymc.api.entity.ai.MobAiHooks;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.ai.sensor.Sensor;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.player.GameMode;
import org.allaymc.server.entity.ai.executor.EntityControlHelper;

/**
 * Enderman'a dik dik bakan oyuncuyu hedef yapar — HeartCore {@code Enderman::updateProvocationState}.
 *
 * <p>On alti blok icinde, bakisi endermanin gozune {@value #LOOK_DOT_THRESHOLD} kosinusla
 * yonelen ilk gecerli oyuncu {@code ATTACK_TARGET} olur. Boylece enderman en yakin oyuncuyu degil,
 * kendisine bakani kovalar.</p>
 */
public class EndermanStareSensor implements Sensor {

    public static final double LOOK_DOT_THRESHOLD = 0.985;
    protected static final double RANGE_SQUARED = 256.0;

    protected final int period;

    /**
     * @param period tarama periyodu (tick); HeartCore bosta 10, kovalarken 20 tarardi
     */
    public EndermanStareSensor(int period) {
        this.period = period;
    }

    @Override
    public void sense(EntityIntelligent entity) {
        var loc = entity.getLocation();
        double eyeY = loc.y() + entity.getEyeHeight();
        for (var player : entity.getDimension().getPlayers()) {
            var playerEntity = player.getControlledEntity();
            if (playerEntity == null || playerEntity.isDead() || playerEntity.isPhantom()) {
                continue;
            }
            var mode = playerEntity.getGameMode();
            if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
                continue;
            }
            if (!MobAiHooks.canTarget(entity, playerEntity)) {
                continue;
            }
            double distanceSquared = loc.distanceSquared(playerEntity.getLocation());
            if (distanceSquared > RANGE_SQUARED || distanceSquared < 0.01) {
                continue;
            }
            if (EntityControlHelper.isLookingAt(playerEntity, loc.x(), eyeY, loc.z(), LOOK_DOT_THRESHOLD)) {
                entity.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, playerEntity.getRuntimeId());
                return;
            }
        }
    }

    @Override
    public int getPeriod() {
        return period;
    }
}
