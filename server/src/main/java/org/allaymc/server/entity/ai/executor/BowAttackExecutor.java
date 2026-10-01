package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Iskeletin yay atisi — HeartCore {@code RangedBowAttackStrategy} ayarlari.
 *
 * <p>Yay 15 blok icinde atilir; mob menzilin icindeyken yerinde durur, disindaysa yaklasir
 * (geri cekilme yok). Gerilme 20-30 tick surer ve her atistan sonraki bekleme hedefe uzakliga
 * gore 1-3 saniyedir; ikisi ayni anda isledigi icin atis araligi {@code max(bekleme, germe)}'dir.
 * Ok cikis hizi {@code 1.15 + min(0.55, mesafe * 0.025)}, nisan hedefin goz yuksekliginin %60'i,
 * dusus {@code 0.03 * mesafe} yukari telafi edilir ve mesafeyle azalan rastgele sapma eklenir.</p>
 */
public class BowAttackExecutor extends RangedAttackExecutor {

    protected static final double ATTACK_RADIUS = 15.0;
    protected static final int MIN_INTERVAL_TICKS = 20;
    protected static final int MAX_INTERVAL_TICKS = 60;
    protected static final int MIN_DRAW_TICKS = 20;
    protected static final int MAX_DRAW_TICKS = 30;
    protected static final double SPREAD_BASE = 0.045;

    protected int nextCharge;

    public BowAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                             boolean clearTargetAfterLose) {
        super(targetIdMemory, speed, maxSenseRange, ATTACK_RADIUS, 0, clearTargetAfterLose,
                MIN_INTERVAL_TICKS);
        this.nextCharge = rollCharge(ATTACK_RADIUS);
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        super.onStart(entity);
        nextCharge = rollCharge(ATTACK_RADIUS);
    }

    @Override
    protected int chargeTicks() {
        return nextCharge;
    }

    @Override
    protected void onShot(double distance) {
        nextCharge = rollCharge(distance);
    }

    /** Atistan sonraki toplam bekleme eksi nisan suresi; en az 1 tick. */
    protected int rollCharge(double distance) {
        double ratio = Math.min(1.0, distance / ATTACK_RADIUS);
        int interval = (int) (MIN_INTERVAL_TICKS + (MAX_INTERVAL_TICKS - MIN_INTERVAL_TICKS) * ratio);
        int draw = ThreadLocalRandom.current().nextInt(MIN_DRAW_TICKS, MAX_DRAW_TICKS + 1);
        return Math.max(1, Math.max(interval, draw) - aimTicks());
    }

    @Override
    protected double arrowVelocityFor(double distance) {
        return velocity(distance);
    }

    /** HeartCore {@code calculateVelocity}. */
    public static double velocity(double distance) {
        return 1.15 + Math.min(0.55, distance * 0.025);
    }

    @Override
    protected double aimHeightFactor() {
        return 0.6;
    }

    @Override
    protected void adjustDirection(Vector3d direction, double distance) {
        // Ok yolda duser: hedefin biraz ustune nisan al.
        direction.add(0.0, 0.03 * distance, 0.0);
        var rand = ThreadLocalRandom.current();
        double spread = Math.max(0.01, SPREAD_BASE * (1.0 - Math.min(0.8, distance / 20.0))) * distance;
        direction.add(
                (rand.nextDouble() * 2 - 1) * spread,
                (rand.nextDouble() * 2 - 1) * spread * 0.65,
                (rand.nextDouble() * 2 - 1) * spread);
    }
}
