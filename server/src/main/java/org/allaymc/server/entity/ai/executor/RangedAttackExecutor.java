package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.action.SimpleEntityAction;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.data.WeaponStance;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.component.ItemCrossbowBaseComponent;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

/**
 * Hafizada tutulan hedefe karsi menzilli kovala-ve-ates-et davranisini yurutur.
 *
 * <p>Yay ya da arbaletle dovusen moblar icin {@link MeleeAttackExecutor} karsiligidir. Mob
 * sonuna kadar yaklasmak yerine hedefi bir mesafe bandi icinde tutar: hedef
 * {@code preferredRange} otesindeyse yaklasir, {@code minRange} degerinden yakina girerse geri
 * cekilir; boylece oyuncu oylece yanina yurup dikilemez.</p>
 */
public class RangedAttackExecutor implements BehaviorExecutor {

    /** Atis yapilmadan once silahin hedefe tam gerilmis halde tutuldugu tick sayisi. */
    protected static final int AIM_TIME = 10;

    protected final MemoryType<Long> targetIdMemory;
    protected final float speed;
    protected final double maxSenseRangeSquared;
    protected final double preferredRangeSquared;
    protected final double minRangeSquared;
    protected final boolean clearTargetAfterLose;
    protected final int coolDown;
    protected final float arrowVelocity;
    protected final float arrowBaseDamage;

    protected int attackTick;
    protected Vector3d lastTargetPos;

    /**
     * Bir menzilli saldiri executor'u olusturur.
     *
     * @param targetIdMemory hedef varligin calisma zamani kimligini tutan hafiza gozu
     * @param speed konum degistirirken kullanilan hareket hizi
     * @param maxSenseRange hedefin takip edilebilecegi en fazla mesafe (blok)
     * @param preferredRange mobun ates etmeye calistigi mesafe (blok)
     * @param minRange altina inilince mobun geri cekildigi mesafe (blok)
     * @param clearTargetAfterLose davranis durdugunda hedef hafizasinin temizlenip temizlenmeyecegi
     * @param coolDown atislar arasindaki bekleme (tick)
     */
    public RangedAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                double preferredRange, double minRange,
                                boolean clearTargetAfterLose, int coolDown) {
        this(targetIdMemory, speed, maxSenseRange, preferredRange, minRange, clearTargetAfterLose, coolDown, 2.5f, 2f);
    }

    /**
     * Ozel mermi degerleriyle bir menzilli saldiri executor'u olusturur.
     *
     * @param targetIdMemory hedef varligin calisma zamani kimligini tutan hafiza gozu
     * @param speed konum degistirirken kullanilan hareket hizi
     * @param maxSenseRange hedefin takip edilebilecegi en fazla mesafe (blok)
     * @param preferredRange mobun ates etmeye calistigi mesafe (blok)
     * @param minRange altina inilince mobun geri cekildigi mesafe (blok)
     * @param clearTargetAfterLose davranis durdugunda hedef hafizasinin temizlenip temizlenmeyecegi
     * @param coolDown atislar arasindaki bekleme (tick)
     * @param arrowVelocity firlatilan okun cikis hizi
     * @param arrowBaseDamage firlatilan okun taban hasari
     */
    public RangedAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                double preferredRange, double minRange,
                                boolean clearTargetAfterLose, int coolDown,
                                float arrowVelocity, float arrowBaseDamage) {
        this.targetIdMemory = targetIdMemory;
        this.speed = speed;
        this.maxSenseRangeSquared = maxSenseRange * maxSenseRange;
        this.preferredRangeSquared = preferredRange * preferredRange;
        this.minRangeSquared = minRange * minRange;
        this.clearTargetAfterLose = clearTargetAfterLose;
        this.coolDown = coolDown;
        this.arrowVelocity = arrowVelocity;
        this.arrowBaseDamage = arrowBaseDamage;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        attackTick = 0;
        lastTargetPos = null;
        entity.setMovementSpeed(speed);
        entity.setPitchEnabled(true);
        setWeaponStance(entity, WeaponStance.CHARGING);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        attackTick++;

        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        if (targetId == null) {
            return false;
        }

        var targetEntity = entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(targetEntity instanceof EntityLiving) || !isTargetValid(targetEntity)) {
            return false;
        }

        var entityLoc = entity.getLocation();
        var targetLoc = targetEntity.getLocation();
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

        // Yerinde dururken ya da geri cekilirken bile hep hedefe don.
        EntityControlHelper.setLookTarget(entity, new Vector3d(
                targetLoc.x(), targetLoc.y() + targetEntity.getEyeHeight(), targetLoc.z()
        ));

        updateMovement(entity, entityLoc.x(), entityLoc.z(), targetLoc.x(), targetLoc.y(), targetLoc.z(), distanceSquared);

        // Ger, hedefte tut, sonra birak. Tutma asamasi istemcinin tam gerilmis pozu gosterebilmesi
        // icin var; germe biter bitmez ates etmek ekranda bir segirme gibi gorunuyor.
        if (attackTick < coolDown) {
            setWeaponStance(entity, WeaponStance.CHARGING);
            setCrossbowLoaded(entity, false);
        } else if (attackTick < coolDown + AIM_TIME) {
            setWeaponStance(entity, WeaponStance.READY);
            setCrossbowLoaded(entity, true);
        } else {
            shoot(entity, targetEntity);
            attackTick = 0;
            setWeaponStance(entity, WeaponStance.CHARGING);
            setCrossbowLoaded(entity, false);
        }

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setPitchEnabled(false);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        lastTargetPos = null;
        // Silahi indir; yoksa mob tam gerilmis yayla dolasmaya devam eder.
        setWeaponStance(entity, WeaponStance.IDLE);
        setCrossbowLoaded(entity, false);
        if (clearTargetAfterLose) {
            entity.getMemoryStorage().clear(targetIdMemory);
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    /**
     * Hedef cok uzaksa ona dogru, cok yakinsa ondan uzaga yurur; hedef ikisinin arasindaki rahat
     * bantta durdugu surece hareket etmez.
     */
    protected void updateMovement(EntityIntelligent entity, double entityX, double entityZ,
                                  double targetX, double targetY, double targetZ, double distanceSquared) {
        Vector3d moveTarget;
        if (distanceSquared > preferredRangeSquared) {
            moveTarget = new Vector3d(targetX, targetY, targetZ);
        } else if (distanceSquared < minRangeSquared) {
            // Hedeften dogruca uzaga adim at, kapatacagimiz mesafeyi koruyarak.
            var dx = entityX - targetX;
            var dz = entityZ - targetZ;
            var length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1e-4) {
                // Tam hedefin ustunde duruyoruz: her yon olur, birini sec.
                dx = 1;
                dz = 0;
                length = 1;
            }
            var retreat = Math.sqrt(preferredRangeSquared);
            moveTarget = new Vector3d(entityX + dx / length * retreat, targetY, entityZ + dz / length * retreat);
        } else {
            // Rahat mesafe: yerinde kal ve ates etmeye devam et.
            EntityControlHelper.removeRouteTarget(entity);
            lastTargetPos = null;
            return;
        }

        entity.setMoveTarget(moveTarget);
        if (lastTargetPos == null || isInDifferentBlock(lastTargetPos, moveTarget)) {
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }
        lastTargetPos = moveTarget;
    }

    protected void shoot(EntityIntelligent entity, Entity target) {
        var dimension = entity.getDimension();
        var location = entity.getLocation();
        var shootPos = new Vector3d(location.x(), location.y() + entity.getEyeHeight() - 0.1, location.z());

        // Ayaklarina degil gozlerine nisan al; yoksa her ok hedefin onune duser.
        var targetLoc = target.getLocation();
        var direction = new Vector3d(
                targetLoc.x() - shootPos.x(),
                targetLoc.y() + target.getEyeHeight() - shootPos.y(),
                targetLoc.z() - shootPos.z()
        );
        if (direction.lengthSquared() < 1e-6) {
            direction = MathUtils.getDirectionVector(location);
        } else {
            direction.normalize();
        }

        var arrow = EntityTypes.ARROW.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(shootPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(direction.mul(arrowVelocity))
                        .build()
        );
        arrow.setShooter(entity);
        arrow.setBaseDamage(arrowBaseDamage);
        // Mob oklari toplanamaz; yoksa bir mob ciftligi yeri oka bogar.
        arrow.setPickUpDisabled(true);
        dimension.getEntityManager().addEntity(arrow);

        entity.applyAction(SimpleEntityAction.SWING_ARM);
        // Yay geren bir iskelet, arbalet mandalinin birakilmasi gibi ses cikarmamali.
        dimension.addSound(shootPos, holdsCrossbow(entity) ? SimpleSound.CROSSBOW_SHOOT : SimpleSound.BOW_SHOOT);
    }

    /**
     * @return mobun yay yerine arbalet kullanip kullanmadigi. Hem atis sesine hem de yuklenecek
     * bir arbalet olup olmadigina bu karar verir.
     */
    protected boolean holdsCrossbow(EntityIntelligent entity) {
        return heldCrossbow(entity) != null;
    }

    /**
     * @return mobun tuttugu arbalet; baska bir sey tutuyorsa {@code null}
     */
    protected ItemCrossbowBaseComponent heldCrossbow(EntityIntelligent entity) {
        if (!(entity instanceof EntityContainerHolderComponent containerHolder)) {
            return null;
        }

        var handContainer = containerHolder.getContainer(ContainerTypes.ENTITY_HAND);
        if (handContainer == null) {
            return null;
        }

        return handContainer.getItemInHand() instanceof ItemCrossbowBaseComponent crossbow ? crossbow : null;
    }

    /**
     * Silah asamasini varliga yansitir ki istemci canlandirabilsin. Bilesen yalnizca deger
     * gercekten degistiginde yayin yaptigi icin bunu her tick cagirmak bedavadir.
     */
    protected void setWeaponStance(EntityIntelligent entity, WeaponStance stance) {
        if (entity instanceof EntityWeaponStanceComponent weaponStance) {
            weaponStance.setWeaponStance(stance);
        }
    }

    /**
     * Mobun tuttugu arbaleti doldurur ya da bosaltir.
     *
     * <p>Varlik bayraklari tek basina bir arbaleti gerilmis gostermeye yetmez: buna istemci
     * esyanin kendisine bakarak karar verir ve arbalet ancak uzerinde {@code chargedItem} etiketi
     * varken dolu cizilir. Yani mob kurmayi bitirdiginde ok gercekten arbaletine konmali, ates
     * ettiginde de geri alinmali; aksi halde silah butun dovus boyunca gevsek gorunur.</p>
     *
     * <p>Yigini yerinde degistirmek el konteynerinin slot dinleyicisini calistirmadigi icin yeni
     * esyanin izleyicilere gitmesi adina slot acikca bildiriliyor.</p>
     */
    protected void setCrossbowLoaded(EntityIntelligent entity, boolean loaded) {
        var crossbow = heldCrossbow(entity);
        if (crossbow == null || crossbow.isLoaded() == loaded) {
            return;
        }

        crossbow.setLoadedProjectile(loaded ? ItemTypes.ARROW.createItemStack() : null);
        ((EntityContainerHolderComponent) entity).getContainer(ContainerTypes.ENTITY_HAND).notifySlotChange(0);
    }

    protected boolean isInDifferentBlock(Vector3d oldTargetPos, Vector3d newTargetPos) {
        return Math.floor(oldTargetPos.x()) != Math.floor(newTargetPos.x()) ||
               Math.floor(oldTargetPos.y()) != Math.floor(newTargetPos.y()) ||
               Math.floor(oldTargetPos.z()) != Math.floor(newTargetPos.z());
    }

    protected boolean isTargetValid(Entity targetEntity) {
        if (!targetEntity.isAlive()) {
            return false;
        }

        if (targetEntity instanceof EntityPlayer player) {
            var gameMode = player.getGameMode();
            return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
        }

        return true;
    }
}
