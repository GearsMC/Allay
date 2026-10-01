package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.ai.memory.MemoryType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Orumcek saldirisi: yakin dovusun ustune hedefe dogru sicrayis.
 *
 * <p>HeartCore {@code Spider::tryLeapAtTarget} ile ayni kural: hedef iki ile alti blok
 * arasindayken, yerdeyken ve bekleme bittiyse %65 olasilikla hedefe dogru sicrar
 * (yatay 0.7, dikey 0.42), ardindan 35 tick bekler. Sicrayis dusme mesafesini sifirlar.</p>
 */
public class SpiderAttackExecutor extends MeleeAttackExecutor {

    protected static final int LEAP_COOLDOWN = 35;
    protected static final double LEAP_MIN_DISTANCE_SQUARED = 4.0;
    protected static final double LEAP_MAX_DISTANCE_SQUARED = 36.0;
    protected static final double LEAP_CHANCE = 0.65;
    protected static final double LEAP_HORIZONTAL = 0.7;
    protected static final double LEAP_VERTICAL = 0.42;

    protected int leapCooldown;

    public SpiderAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                boolean clearTargetAfterLose, int coolDown, double attackRange) {
        super(targetIdMemory, speed, maxSenseRange, clearTargetAfterLose, coolDown, attackRange);
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        super.onStart(entity);
        leapCooldown = 0;
    }

    @Override
    protected void onChase(EntityIntelligent entity, EntityLiving target, double distanceSquared) {
        if (leapCooldown > 0) {
            leapCooldown--;
            return;
        }
        if (!entity.isOnGround()
                || distanceSquared < LEAP_MIN_DISTANCE_SQUARED || distanceSquared > LEAP_MAX_DISTANCE_SQUARED
                || ThreadLocalRandom.current().nextDouble() > LEAP_CHANCE) {
            return;
        }

        var from = entity.getLocation();
        var to = target.getLocation();
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal <= 0.0001) {
            return;
        }

        entity.setMotion(dx / horizontal * LEAP_HORIZONTAL, LEAP_VERTICAL, dz / horizontal * LEAP_HORIZONTAL);
        entity.resetFallDistance();
        leapCooldown = LEAP_COOLDOWN;
    }
}
