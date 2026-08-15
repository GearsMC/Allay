package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

/**
 * Blaze tarzi ates topu saldirisi: hedefin yakininda havada durup ona kucuk ates topu serisi savurur.
 *
 * <p>Blaze'i taninir kilan sey bu ritim oldugu icin acikca modellendi: hedef {@code chargeTime}
 * tick boyunca menzilde kaldiktan sonra mob birkac tick arayla {@code burstSize} kadar ates topu
 * atar, sonra yeniden sarj etmeden once {@code coolDown} tick susar. Mob ayrica yay kullanan
 * moblar gibi bir mesafe bandi korur ve hedefin ayaklarina inmek yerine ustunde suzulur; havada
 * olmak blaze'in butun avantajidir.</p>
 *
 * <p>Merminin kendisi icin burada is yok: {@code SMALL_FIREBALL} hasarini zaten veriyor, kurbani
 * tutusturuyor ve dustugu yerde yangin cikariyor.</p>
 */
public class FireballAttackExecutor implements BehaviorExecutor {

    /** Bir serideki ates toplari arasindaki tick sayisi. */
    protected static final int SHOT_INTERVAL = 6;

    /** Mobun hedefin kac blok ustunde durmaya calistigi. */
    protected static final double HOVER_HEIGHT = 2.0;

    /** Ates topunun cikis hizi. */
    protected static final float FIREBALL_SPEED = 0.6f;

    protected final MemoryType<Long> targetIdMemory;
    protected final float speed;
    protected final double maxSenseRangeSquared;
    protected final double preferredRangeSquared;
    protected final double minRangeSquared;
    protected final boolean clearTargetAfterLose;
    protected final int chargeTime;
    protected final int coolDown;
    protected final int burstSize;

    protected int tick;
    protected int shotsLeft;
    protected int nextShotTick;
    protected Vector3d lastMoveTarget;

    /**
     * Bir ates topu saldiri executor'u olusturur.
     *
     * @param targetIdMemory hedef varligin calisma zamani kimligini tutan hafiza gozu
     * @param speed konum degistirirken kullanilan ucus hizi
     * @param maxSenseRange hedefin takip edilebilecegi en fazla mesafe (blok)
     * @param preferredRange mobun saldirmaya calistigi mesafe (blok)
     * @param minRange altina inilince mobun geri cekildigi mesafe (blok)
     * @param clearTargetAfterLose davranis durdugunda hedef hafizasinin temizlenip temizlenmeyecegi
     * @param chargeTime seri oncesi sarj icin harcanan tick sayisi
     * @param coolDown seriden sonraki sessizlik (tick)
     * @param burstSize bir serideki ates topu sayisi
     */
    public FireballAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                  double preferredRange, double minRange, boolean clearTargetAfterLose,
                                  int chargeTime, int coolDown, int burstSize) {
        this.targetIdMemory = targetIdMemory;
        this.speed = speed;
        this.maxSenseRangeSquared = maxSenseRange * maxSenseRange;
        this.preferredRangeSquared = preferredRange * preferredRange;
        this.minRangeSquared = minRange * minRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
        this.chargeTime = chargeTime;
        this.coolDown = coolDown;
        this.burstSize = burstSize;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tick = 0;
        shotsLeft = 0;
        nextShotTick = chargeTime;
        lastMoveTarget = null;
        entity.setMovementSpeed(speed);
        entity.setPitchEnabled(true);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        tick++;

        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        if (targetId == null) {
            return false;
        }

        var target = entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityLiving) || !isTargetValid(target)) {
            return false;
        }

        var entityLoc = entity.getLocation();
        var targetLoc = target.getLocation();
        var distanceSquared = entityLoc.distanceSquared(targetLoc);
        if (distanceSquared > maxSenseRangeSquared) {
            return false;
        }

        if (!entity.isPitchEnabled()) {
            entity.setPitchEnabled(true);
        }
        if (entity.getMovementSpeed() != speed) {
            entity.setMovementSpeed(speed);
        }

        EntityControlHelper.setLookTarget(entity, new Vector3d(
                targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()
        ));

        updateMovement(entity, entityLoc.x(), entityLoc.z(),
                targetLoc.x(), targetLoc.y(), targetLoc.z(), distanceSquared);

        if (tick >= nextShotTick) {
            if (shotsLeft == 0) {
                shotsLeft = burstSize;
            }

            shoot(entity, target);
            shotsLeft--;
            nextShotTick = tick + (shotsLeft > 0 ? SHOT_INTERVAL : coolDown + chargeTime);
        }

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setPitchEnabled(false);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        lastMoveTarget = null;
        if (clearTargetAfterLose) {
            entity.getMemoryStorage().clear(targetIdMemory);
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    /**
     * Hedef uzaksa yaklasir, cok yakinsa geri suzulur, arada ise yerini korur; her zaman hedefin
     * hizasini degil ustundeki bir noktayi hedefler.
     */
    protected void updateMovement(EntityIntelligent entity, double entityX, double entityZ,
                                  double targetX, double targetY, double targetZ, double distanceSquared) {
        double hoverY = targetY + HOVER_HEIGHT;
        Vector3d moveTarget;

        if (distanceSquared > preferredRangeSquared) {
            moveTarget = new Vector3d(targetX, hoverY, targetZ);
        } else if (distanceSquared < minRangeSquared) {
            var dx = entityX - targetX;
            var dz = entityZ - targetZ;
            var length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1e-4) {
                dx = 1;
                dz = 0;
                length = 1;
            }
            var retreat = Math.sqrt(preferredRangeSquared);
            moveTarget = new Vector3d(entityX + dx / length * retreat, hoverY, entityZ + dz / length * retreat);
        } else {
            // Atis pozisyonu korunuyor, ama asagi kaydiysak suzulme yuksekligine geri tirmaniliyor.
            moveTarget = new Vector3d(entityX, hoverY, entityZ);
        }

        entity.setMoveTarget(moveTarget);
        if (lastMoveTarget == null || isInDifferentBlock(lastMoveTarget, moveTarget)) {
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }
        lastMoveTarget = moveTarget;
    }

    protected void shoot(EntityIntelligent entity, Entity target) {
        var dimension = entity.getDimension();
        var location = entity.getLocation();
        var shootPos = new Vector3d(location.x(), location.y() + entity.getEyeHeight() - 0.1, location.z());

        var targetLoc = target.getLocation();
        var direction = new Vector3d(
                targetLoc.x() - shootPos.x(),
                targetLoc.y() + target.getEyeHeight() - shootPos.y(),
                targetLoc.z() - shootPos.z()
        );
        if (direction.lengthSquared() < 1e-6) {
            return;
        }
        direction.normalize();

        var fireball = EntityTypes.SMALL_FIREBALL.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(shootPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(direction.mul(FIREBALL_SPEED))
                        .build()
        );
        fireball.setShooter(entity);
        dimension.getEntityManager().addEntity(fireball);

        dimension.addSound(shootPos, SimpleSound.FIRE_CHARGE);
    }

    protected boolean isInDifferentBlock(Vector3d oldTarget, Vector3d newTarget) {
        return Math.floor(oldTarget.x()) != Math.floor(newTarget.x()) ||
               Math.floor(oldTarget.y()) != Math.floor(newTarget.y()) ||
               Math.floor(oldTarget.z()) != Math.floor(newTarget.z());
    }

    protected boolean isTargetValid(Entity target) {
        if (!target.isAlive()) {
            return false;
        }

        if (target instanceof EntityPlayer player) {
            var gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
        }

        return true;
    }
}
