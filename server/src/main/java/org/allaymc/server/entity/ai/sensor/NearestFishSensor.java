package org.allaymc.server.entity.ai.sensor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.ai.sensor.Sensor;
import org.allaymc.api.entity.interfaces.EntityCod;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPufferfish;
import org.allaymc.api.entity.interfaces.EntitySalmon;
import org.allaymc.api.entity.interfaces.EntityTropicalfish;

/**
 * Menzildeki en yakin baligi arar ve hafizaya yazar.
 *
 * <p>Akselotun avini bulma yolu. Oyuncu arayan sensorun aksine burada butun boyuttaki varliklar
 * taraniyor; balik sayisi az oldugu icin bu tarama, sensor periyoduyla seyreltildiginde ucuz
 * kaliyor.</p>
 */
public class NearestFishSensor implements Sensor {

    protected final double range;
    protected final int period;

    /**
     * @param range balik aranacak en fazla mesafe (blok)
     * @param period iki tarama arasindaki tick sayisi
     */
    public NearestFishSensor(double range, int period) {
        this.range = range;
        this.period = period;
    }

    @Override
    public void sense(EntityIntelligent entity) {
        var loc = entity.getLocation();
        double rangeSq = range * range;

        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (var candidate : entity.getDimension().getEntities().values()) {
            if (!isFish(candidate) || !candidate.isAlive()) {
                continue;
            }

            double distSq = loc.distanceSquared(candidate.getLocation());
            if (distSq > rangeSq || distSq >= nearestDistSq) {
                continue;
            }

            nearestDistSq = distSq;
            nearest = candidate;
        }

        entity.getMemoryStorage().put(MemoryTypes.NEAREST_FISH, nearest != null ? nearest.getRuntimeId() : null);
    }

    protected boolean isFish(Entity entity) {
        return entity instanceof EntityCod
               || entity instanceof EntitySalmon
               || entity instanceof EntityTropicalfish
               || entity instanceof EntityPufferfish;
    }

    @Override
    public int getPeriod() {
        return period;
    }
}
