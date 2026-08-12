package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.data.PotionType;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Cadi saldirisi: hedefe atilabilir iksir firlatir.
 *
 * <p>Hangi iksirin atilacagi yazi tura degil vanilla'nin siralamasini izler, cunku cadiyi bilincli
 * hissettiren sey budur: mesafesini koruyani yavaslatir, sagligi yerinde hedefi zehirler, yakin
 * dovuse gireni gucsuzlestirir, geri kalan durumlarda duz hasara doner. Ayni etkiyi iki kez
 * atmaz; hedefte zaten var olan bir etkiyi yalnizca tazeleyecek iksir atlanip bir alt secenege
 * gecilir.</p>
 *
 * <p>Atilabilir iksirler yay cizerek gider, bu yuzden nisan hedefin uzerine mesafeyle birlikte
 * buyuyen bir miktar kaydirilir; duz atmak her iksiri hedefin onune dusururdu.</p>
 */
public class PotionAttackExecutor implements BehaviorExecutor {

    /** Firlatilan iksirin cikis hizi. */
    protected static final float POTION_SPEED = 0.5f;

    /** Yayi telafi etmek icin her yatay blok basina eklenen yukari nisan payi. */
    protected static final double ARC_LIFT_PER_BLOCK = 0.22;

    /** Bu canin altinda hedef zehir harcamaya degmeyecek kadar yarali sayilir. */
    protected static final float POISON_HEALTH_THRESHOLD = 8;

    /** Otesinde cadinin hedefi yavaslatmayi tercih ettigi mesafe. */
    protected static final double SLOWNESS_RANGE = 8;

    /** Icinde cadinin hedefi gucsuzlestirmeyi tercih ettigi mesafe. */
    protected static final double WEAKNESS_RANGE = 3;

    protected final MemoryType<Long> targetIdMemory;
    protected final float speed;
    protected final double maxSenseRangeSquared;
    protected final double preferredRangeSquared;
    protected final double minRangeSquared;
    protected final boolean clearTargetAfterLose;
    protected final int coolDown;

    protected int tick;
    protected Vector3d lastMoveTarget;

    /**
     * Bir iksir saldiri executor'u olusturur.
     *
     * @param targetIdMemory hedef varligin calisma zamani kimligini tutan hafiza gozu
     * @param speed konum degistirirken kullanilan hareket hizi
     * @param maxSenseRange hedefin takip edilebilecegi en fazla mesafe (blok)
     * @param preferredRange cadinin atmaya calistigi mesafe (blok)
     * @param minRange altina inilince cadinin geri cekildigi mesafe (blok)
     * @param clearTargetAfterLose davranis durdugunda hedef hafizasinin temizlenip temizlenmeyecegi
     * @param coolDown iki atis arasindaki bekleme (tick)
     */
    public PotionAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                double preferredRange, double minRange,
                                boolean clearTargetAfterLose, int coolDown) {
        this.targetIdMemory = targetIdMemory;
        this.speed = speed;
        this.maxSenseRangeSquared = maxSenseRange * maxSenseRange;
        this.preferredRangeSquared = preferredRange * preferredRange;
        this.minRangeSquared = minRange * minRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
        this.coolDown = coolDown;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tick = 0;
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
        if (!(target instanceof EntityLiving targetLiving) || !isTargetValid(target)) {
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

        if (tick > coolDown) {
            throwPotion(entity, target, choosePotion(targetLiving, Math.sqrt(distanceSquared)));
            tick = 0;
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
     * Bu hedef icin vanilla'nin sececegi iksiri secer; hedefte zaten olan bir etkiyi atlar.
     */
    protected PotionType choosePotion(EntityLivingComponent target, double distance) {
        if (distance > SLOWNESS_RANGE && !target.hasEffect(EffectTypes.SLOWNESS)) {
            return PotionType.SLOWNESS;
        }

        if (target.getHealth() >= POISON_HEALTH_THRESHOLD && !target.hasEffect(EffectTypes.POISON)) {
            return PotionType.POISON;
        }

        if (distance <= WEAKNESS_RANGE && !target.hasEffect(EffectTypes.WEAKNESS)
            && ThreadLocalRandom.current().nextInt(4) == 0) {
            return PotionType.WEAKNESS;
        }

        return PotionType.HARMING;
    }

    protected void updateMovement(EntityIntelligent entity, double entityX, double entityZ,
                                  double targetX, double targetY, double targetZ, double distanceSquared) {
        Vector3d moveTarget;
        if (distanceSquared > preferredRangeSquared) {
            moveTarget = new Vector3d(targetX, targetY, targetZ);
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
            moveTarget = new Vector3d(entityX + dx / length * retreat, targetY, entityZ + dz / length * retreat);
        } else {
            EntityControlHelper.removeRouteTarget(entity);
            lastMoveTarget = null;
            return;
        }

        entity.setMoveTarget(moveTarget);
        if (lastMoveTarget == null || isInDifferentBlock(lastMoveTarget, moveTarget)) {
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }
        lastMoveTarget = moveTarget;
    }

    protected void throwPotion(EntityIntelligent entity, Entity target, PotionType potionType) {
        var dimension = entity.getDimension();
        var location = entity.getLocation();
        var throwPos = new Vector3d(location.x(), location.y() + entity.getEyeHeight() - 0.1, location.z());

        var targetLoc = target.getLocation();
        double dx = targetLoc.x() - throwPos.x();
        double dz = targetLoc.z() - throwPos.z();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        // Hedefin ustune nisan al; iksirler yolda duser, yani hedef uzaklastikca daha yukari atariz.
        double dy = targetLoc.y() - throwPos.y() + horizontalDistance * ARC_LIFT_PER_BLOCK;

        var direction = new Vector3d(dx, dy, dz);
        if (direction.lengthSquared() < 1e-6) {
            return;
        }
        direction.normalize();

        var potion = EntityTypes.SPLASH_POTION.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(throwPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(direction.mul(POTION_SPEED))
                        .build()
        );
        potion.setShooter(entity);
        potion.setPotionType(potionType);
        dimension.getEntityManager().addEntity(potion);

        dimension.addSound(throwPos, SimpleSound.ITEM_THROW);
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
