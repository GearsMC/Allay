package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tavsanin temel davranisi — HeartCore {@code Rabbit}: kucuk model, deri cesidi ve ziplayarak ilerleme.
 *
 * <p><b>Model:</b> PHP modeli {@code 0.65} olcekle cizip carpisma kutusunu {@code 1/0.65} ile
 * genisletiyordu; sonuc vanilla kutu, daha kucuk bir tavsandi. Ayni sey burada olcek ve taban
 * kutusuyla yapilir.</p>
 *
 * <p><b>Deri:</b> alti dogal cesit ve {@code 1/500} olasilikla katil tavsan ({@code 99});
 * kayit NBT'dedir.</p>
 *
 * <p><b>Ziplama:</b> yerde ve hareket halindeyken {@code 6-14} tick'te bir (kacarken {@code 6})
 * {@code 0.42} dikey itme ve yatay hiz takviyesi (normal {@code 1.1x}, kacarken {@code 1.35x});
 * istemci ziplama animasyonunu {@code JUMP_GOAL_JUMP} bayragi ve sureyle oynatir.</p>
 */
public class EntityRabbitBaseComponentImpl extends EntityBaseComponentImpl {

    public static final double MODEL_SCALE = 0.65;
    /** Dolasma ve kacis hizlari; ziplama kacisi bu esikten tanir. */
    public static final float ROAM_SPEED = 0.15f;
    public static final float FLEE_SPEED = 0.22f;

    public static final int VARIANT_BROWN = 0;
    public static final int VARIANT_SALT = 5;
    public static final int VARIANT_KILLER = 99;

    protected static final String TAG_VARIANT = "Variant";
    protected static final double VANILLA_HALF_WIDTH = 0.2;
    protected static final double VANILLA_HEIGHT = 0.5;
    protected static final double HOP_VERTICAL = 0.42;
    protected static final double MIN_HOP_SPEED_SQUARED = 0.01;
    protected static final int HOP_INTERVAL_MIN = 6;
    protected static final int HOP_INTERVAL_MAX = 14;
    protected static final int HOP_ANIMATION_TICKS = 8;
    protected static final int FLEE_HOP_ANIMATION_TICKS = 10;

    @Dependency
    protected EntityPhysicsComponent physicsComponent;
    @Dependency
    protected EntityAIComponent aiComponent;

    protected volatile int variant = VARIANT_BROWN;
    protected int hopCooldown;
    protected int hopTicks;
    protected boolean broadcastHopping;

    public EntityRabbitBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        double half = VANILLA_HALF_WIDTH / MODEL_SCALE;
        return new AABBd(-half, 0.0, -half, half, VANILLA_HEIGHT / MODEL_SCALE, half);
    }

    /** @return deri cesidi ({@code 0-5} dogal, {@code 99} katil) */
    public int getVariant() {
        return variant;
    }

    /** @return tavsan su an ziplama animasyonunda mi */
    public boolean isHopping() {
        return hopTicks > 0;
    }

    /** @return kalan ziplama animasyonu tick'i (istemci {@code JUMP_DURATION}) */
    public int getHopTicks() {
        return Math.max(0, Math.min(255, hopTicks));
    }

    /** Dogal cesitlerden birini secer; kucuk bir olasilikla katil tavsan. */
    public static int rollVariant() {
        var rand = ThreadLocalRandom.current();
        if (rand.nextInt(500) == 0) {
            return VARIANT_KILLER;
        }
        return rand.nextInt(VARIANT_BROWN, VARIANT_SALT + 1);
    }

    static boolean validVariant(int variant) {
        return variant == VARIANT_KILLER || (variant >= VARIANT_BROWN && variant <= VARIANT_SALT);
    }

    @EventHandler
    protected void onLoadNBT(CEntityLoadNBTEvent event) {
        event.getNbt().listenForInt(TAG_VARIANT, saved -> this.variant = validVariant(saved) ? saved : VARIANT_BROWN);
        if (!event.getNbt().containsKey(TAG_VARIANT)) {
            this.variant = rollVariant();
        }
        setScale(MODEL_SCALE);
    }

    @EventHandler
    protected void onSaveNBT(CEntitySaveNBTEvent event) {
        event.getNbt().putInt(TAG_VARIANT, variant);
    }

    @EventHandler
    protected void onHopTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive()) {
            return;
        }
        if (hopCooldown > 0) {
            hopCooldown--;
        }
        if (hopTicks > 0) {
            hopTicks--;
        }

        boolean fleeing = aiComponent.getMovementSpeed() >= FLEE_SPEED - 0.001f;
        if (physicsComponent.isOnGround() && hopCooldown <= 0) {
            var motion = physicsComponent.getMotion();
            double horizontalSquared = motion.x() * motion.x() + motion.z() * motion.z();
            if (fleeing || horizontalSquared >= MIN_HOP_SPEED_SQUARED) {
                hop(fleeing);
            }
        }

        boolean hopping = hopTicks > 0;
        if (hopping != broadcastHopping) {
            broadcastHopping = hopping;
            broadcastState();
        }
    }

    protected void hop(boolean fleeing) {
        var motion = physicsComponent.getMotion();
        double desired = aiComponent.getMovementSpeed() * (fleeing ? 1.35 : 1.1);
        double horizontal = Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z());
        double mx = motion.x();
        double mz = motion.z();
        if (horizontal > 0.001) {
            double scale = desired / horizontal;
            mx *= scale;
            mz *= scale;
        }
        physicsComponent.setMotion(mx, HOP_VERTICAL, mz);
        hopTicks = fleeing ? FLEE_HOP_ANIMATION_TICKS : HOP_ANIMATION_TICKS;
        hopCooldown = fleeing ? HOP_INTERVAL_MIN
                : ThreadLocalRandom.current().nextInt(HOP_INTERVAL_MIN, HOP_INTERVAL_MAX + 1);
        broadcastHopping = true;
        broadcastState();
    }
}
