package org.allaymc.server.entity.ai.sensor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.sensor.Sensor;
import org.allaymc.api.entity.interfaces.*;
import org.allaymc.api.utils.identifier.Identifier;

public class NearestMonsterSensor implements Sensor {

    public static final MemoryType<Long> NEAREST_MONSTER = new MemoryType<>(new Identifier("gearsmc:nearest_monster"));

    protected final double range;
    protected final int period;

    public NearestMonsterSensor(double range, int period) {
        this.range = range;
        this.period = period;
    }

    public static boolean isMonster(Entity entity) {
        return entity instanceof EntityZombie || entity instanceof EntityHusk || entity instanceof EntityDrowned
               || entity instanceof EntityZombieVillager || entity instanceof EntityZombieVillagerV2
               || entity instanceof EntitySkeleton || entity instanceof EntityStray || entity instanceof EntityBogged
               || entity instanceof EntityParched || entity instanceof EntityWitherSkeleton
               || entity instanceof EntityCreeper || entity instanceof EntitySpider || entity instanceof EntityCaveSpider
               || entity instanceof EntitySilverfish || entity instanceof EntityEndermite || entity instanceof EntityEnderman
               || entity instanceof EntityWitch || entity instanceof EntityPillager || entity instanceof EntityVindicator
               || entity instanceof EntityEvocationIllager || entity instanceof EntityVex || entity instanceof EntityRavager
               || entity instanceof EntityBlaze || entity instanceof EntityGhast || entity instanceof EntitySlime
               || entity instanceof EntityMagmaCube || entity instanceof EntityPhantom || entity instanceof EntityGuardian
               || entity instanceof EntityElderGuardian || entity instanceof EntityZoglin || entity instanceof EntityPiglin
               || entity instanceof EntityPiglinBrute || entity instanceof EntityZombiePigman || entity instanceof EntityBreeze
               || entity instanceof EntityWarden || entity instanceof EntityCreaking || entity instanceof EntityShulker;
    }

    @Override
    public void sense(EntityIntelligent entity) {
        var loc = entity.getLocation();
        var rangeSquared = range * range;
        Entity nearest = null;
        var nearestDistanceSquared = Double.MAX_VALUE;
        for (var candidate : entity.getDimension().getEntities().values()) {
            if (candidate == entity || !candidate.isAlive() || !isMonster(candidate)) {
                continue;
            }

            var distanceSquared = loc.distanceSquared(candidate.getLocation());
            if (distanceSquared > rangeSquared || distanceSquared >= nearestDistanceSquared) {
                continue;
            }

            nearestDistanceSquared = distanceSquared;
            nearest = candidate;
        }

        entity.getMemoryStorage().put(NEAREST_MONSTER, nearest != null ? nearest.getRuntimeId() : null);
    }

    @Override
    public int getPeriod() {
        return period;
    }
}
