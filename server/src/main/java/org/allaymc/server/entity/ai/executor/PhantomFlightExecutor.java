package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.player.GameMode;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fantom ucusu — HeartCore {@code Phantom} sinifindaki tur atma ve dalis dongusu.
 *
 * <p>Hedef varken fantom hedefin etrafinda eliptik bir yorunge cizer (yaricap 5-15 blok,
 * hedefin {@value #CRUISE_ALTITUDE} blok ustunde, hafif dalgali irtifa). Dalis bekleme suresi
 * dolunca ve hedef fantoma bakmiyorsa hedefe dalar, degerse bir kez vurur ve
 * {@value #POST_DIVE_ASCEND_TICKS} tick yukselir; ardindan
 * {@value #DIVE_COOLDOWN}-{@code +}{@value #DIVE_COOLDOWN_RANDOM} tick bekler. Hedef yokken
 * ({@code targetMemory == null}) bulundugu noktanin etrafinda sakin bir yorunge cizer.</p>
 *
 * <p>Hareketi yol bulucu degil dogrudan ivme belirler: gokyuzunde engel azdir, carpismayi fizik
 * yonetir. PHP'deki {@code applyMomentum} ile ayni sekilde hiz hedef hiza her tick belli oranda
 * yaklasir.</p>
 */
public class PhantomFlightExecutor implements BehaviorExecutor {

    protected static final double CRUISE_ALTITUDE = 12.0;
    protected static final double ALTITUDE_WAVE_AMP = 2.0;
    protected static final double ALTITUDE_WAVE_FREQ = 0.008;
    protected static final double SWEEP_RADIUS_MIN = 5.0;
    protected static final double SWEEP_RADIUS_MAX = 15.0;
    protected static final double SWEEP_SPEED_MIN = 0.015;
    protected static final double SWEEP_SPEED_MAX = 0.035;
    protected static final double RADIUS_SHIFT_SPEED = 0.015;
    protected static final double DIRECTION_CHANGE_CHANCE = 0.003;
    protected static final double MOMENTUM_LERP = 0.08;
    protected static final double GLIDE_TURN_SPEED = 0.20;
    protected static final double DIVE_SPEED = 0.55;
    protected static final double ASCEND_SPEED = 0.18;
    protected static final int POST_DIVE_ASCEND_TICKS = 30;
    protected static final int DIVE_COOLDOWN = 160;
    protected static final int DIVE_COOLDOWN_RANDOM = 100;
    protected static final int MAX_DIVE_TICKS = 60;
    protected static final double HIT_DISTANCE = 1.8;
    protected static final double LOOK_DOT_THRESHOLD = 0.9;
    protected static final float BASE_DAMAGE = 6f;

    protected final MemoryType<Long> targetIdMemory;
    protected final double maxSenseRangeSquared;
    protected final boolean clearTargetAfterLose;

    protected enum Phase { CIRCLE, DIVE, ASCEND }

    protected Phase phase;
    protected int phaseTicks;
    protected int diveCooldown;
    protected boolean diveHit;
    protected double sweepAngle;
    protected double sweepSpeed;
    protected int sweepDirection;
    protected double currentRadius;
    protected double targetRadius;
    protected double altitudePhase;
    protected Vector3d orbitCenter;
    protected double cruiseAltitude;

    /**
     * @param targetIdMemory hedef bellegi; {@code null} ise bos gezinme yorungesi
     * @param maxSenseRange hedefin takip edilebilecegi en uzak mesafe
     * @param clearTargetAfterLose davranis durunca hedef bellegi temizlensin mi
     */
    public PhantomFlightExecutor(MemoryType<Long> targetIdMemory, double maxSenseRange, boolean clearTargetAfterLose) {
        this.targetIdMemory = targetIdMemory;
        this.maxSenseRangeSquared = maxSenseRange * maxSenseRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        var rand = ThreadLocalRandom.current();
        phase = Phase.CIRCLE;
        phaseTicks = 0;
        diveHit = false;
        diveCooldown = 40 + rand.nextInt(60);
        sweepAngle = rand.nextDouble() * Math.PI * 2;
        sweepSpeed = SWEEP_SPEED_MIN + rand.nextDouble() * (SWEEP_SPEED_MAX - SWEEP_SPEED_MIN);
        sweepDirection = rand.nextBoolean() ? 1 : -1;
        currentRadius = targetRadius = SWEEP_RADIUS_MIN + rand.nextDouble() * (SWEEP_RADIUS_MAX - SWEEP_RADIUS_MIN);
        altitudePhase = rand.nextDouble() * Math.PI * 2;
        var location = entity.getLocation();
        orbitCenter = new Vector3d(location.x(), location.y(), location.z());
        cruiseAltitude = location.y();
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        Entity target = null;
        if (targetIdMemory != null) {
            var targetId = entity.getMemoryStorage().get(targetIdMemory);
            if (targetId == null) {
                return false;
            }
            target = entity.getDimension().getEntityManager().getEntity(targetId);
            if (!(target instanceof EntityLiving) || !isTargetValid(entity, target)) {
                return false;
            }
            if (entity.getLocation().distanceSquared(target.getLocation()) > maxSenseRangeSquared) {
                return false;
            }
        }

        advanceSweep();
        if (target == null) {
            idleOrbit(entity);
        } else {
            combat(entity, target);
        }
        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        entity.getMemoryStorage().clear(org.allaymc.api.entity.ai.memory.MemoryTypes.MOVE_TARGET);
        if (clearTargetAfterLose && targetIdMemory != null) {
            entity.getMemoryStorage().clear(targetIdMemory);
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    // ---------------------------------------------------------------- savas

    protected void combat(EntityIntelligent entity, Entity target) {
        var targetLoc = target.getLocation();
        var self = entity.getLocation();
        phaseTicks++;
        if (diveCooldown > 0) {
            diveCooldown--;
        }

        switch (phase) {
            case CIRCLE -> {
                orbitCenter.x += (targetLoc.x() - orbitCenter.x) * 0.08;
                orbitCenter.y += (targetLoc.y() - orbitCenter.y) * 0.08;
                orbitCenter.z += (targetLoc.z() - orbitCenter.z) * 0.08;
                cruiseAltitude += ((targetLoc.y() + CRUISE_ALTITUDE) - cruiseAltitude) * 0.04;
                double goalX = orbitCenter.x + Math.cos(sweepAngle) * currentRadius;
                double goalZ = orbitCenter.z + Math.sin(sweepAngle) * currentRadius * 0.7;
                double goalY = cruiseAltitude + Math.sin(altitudePhase) * ALTITUDE_WAVE_AMP;
                glideTo(entity, goalX, goalY, goalZ);
                if (diveCooldown <= 0 && !isLookedAt(target, self.x(), self.y(), self.z())) {
                    phase = Phase.DIVE;
                    phaseTicks = 0;
                    diveHit = false;
                }
            }
            case DIVE -> {
                double goalY = targetLoc.y() + target.getEyeHeight() * 0.5;
                steer(entity, targetLoc.x(), goalY, targetLoc.z(), DIVE_SPEED, 0.35);
                double dx = targetLoc.x() - self.x();
                double dy = goalY - self.y();
                double dz = targetLoc.z() - self.z();
                if (!diveHit && dx * dx + dy * dy + dz * dz <= HIT_DISTANCE * HIT_DISTANCE) {
                    diveHit = true;
                    ((EntityLiving) target).attack(DamageContainer.entityAttack(entity, BASE_DAMAGE));
                    startAscend();
                } else if (phaseTicks > MAX_DIVE_TICKS) {
                    startAscend();
                }
            }
            case ASCEND -> {
                steer(entity, self.x() + (self.x() - targetLoc.x()) * 0.3, self.y() + 6, self.z() + (self.z() - targetLoc.z()) * 0.3,
                        ASCEND_SPEED, MOMENTUM_LERP);
                if (phaseTicks >= POST_DIVE_ASCEND_TICKS) {
                    phase = Phase.CIRCLE;
                    phaseTicks = 0;
                    cruiseAltitude = Math.max(cruiseAltitude, self.y());
                }
            }
        }
        entity.setLookTarget(new Vector3d(targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()));
    }

    protected void startAscend() {
        phase = Phase.ASCEND;
        phaseTicks = 0;
        diveCooldown = DIVE_COOLDOWN + ThreadLocalRandom.current().nextInt(DIVE_COOLDOWN_RANDOM + 1);
    }

    protected void idleOrbit(EntityIntelligent entity) {
        double goalX = orbitCenter.x + Math.cos(sweepAngle) * currentRadius * 1.2;
        double goalZ = orbitCenter.z + Math.sin(sweepAngle) * currentRadius * 0.8;
        double goalY = cruiseAltitude + Math.sin(altitudePhase) * ALTITUDE_WAVE_AMP * 1.3;
        glideTo(entity, goalX, goalY, goalZ);
    }

    // ---------------------------------------------------------------- hareket

    protected void advanceSweep() {
        var rand = ThreadLocalRandom.current();
        if (rand.nextDouble() < DIRECTION_CHANGE_CHANCE) {
            sweepDirection *= -1;
            sweepSpeed = SWEEP_SPEED_MIN + rand.nextDouble() * (SWEEP_SPEED_MAX - SWEEP_SPEED_MIN);
            targetRadius = SWEEP_RADIUS_MIN + rand.nextDouble() * (SWEEP_RADIUS_MAX - SWEEP_RADIUS_MIN);
        }
        sweepAngle += sweepSpeed * sweepDirection;
        currentRadius += (targetRadius - currentRadius) * RADIUS_SHIFT_SPEED;
        altitudePhase += ALTITUDE_WAVE_FREQ;
    }

    /** Yorunge hedefine yumusak suzulme: hiz mesafeyle sinirlanir (PHP {@code GLIDE_TURN_SPEED}). */
    protected void glideTo(EntityIntelligent entity, double x, double y, double z) {
        var loc = entity.getLocation();
        double dx = x - loc.x();
        double dy = y - loc.y();
        double dz = z - loc.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        var motion = entity.getMotion();
        double vx;
        double vz;
        double vy;
        if (horizontal > 0.5) {
            double speed = Math.min(GLIDE_TURN_SPEED, horizontal * 0.06);
            vx = dx / horizontal * speed;
            vz = dz / horizontal * speed;
            vy = dy * 0.08;
        } else {
            vx = motion.x() * 0.7;
            vz = motion.z() * 0.7;
            vy = dy * 0.1;
        }
        applyMomentum(entity, vx, vy, vz, MOMENTUM_LERP);
    }

    /** Sabit hizla noktaya yonel (dalis ve yukselis). */
    protected void steer(EntityIntelligent entity, double x, double y, double z, double speed, double lerp) {
        var loc = entity.getLocation();
        double dx = x - loc.x();
        double dy = y - loc.y();
        double dz = z - loc.z();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.0001) {
            return;
        }
        applyMomentum(entity, dx / length * speed, dy / length * speed, dz / length * speed, lerp);
    }

    protected void applyMomentum(EntityIntelligent entity, double vx, double vy, double vz, double lerp) {
        var motion = entity.getMotion();
        entity.setMotion(
                motion.x() + (vx - motion.x()) * lerp,
                motion.y() + (vy - motion.y()) * lerp,
                motion.z() + (vz - motion.z()) * lerp);
    }

    // ---------------------------------------------------------------- yardimcilar

    /** PHP {@code isPlayerLookingAtMe}: hedef fantoma bakiyorsa dalis ertelenir. */
    protected boolean isLookedAt(Entity target, double phantomX, double phantomY, double phantomZ) {
        return target instanceof EntityPlayer player
                && EntityControlHelper.isLookingAt(player, phantomX, phantomY, phantomZ, LOOK_DOT_THRESHOLD);
    }

    protected boolean isTargetValid(EntityIntelligent entity, Entity target) {
        if (!target.isAlive()) {
            return false;
        }
        if (target instanceof EntityPlayer player) {
            var gameMode = player.getGameMode();
            if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
                return false;
            }
        }
        return EntityControlHelper.allowsTarget(entity, target);
    }
}
