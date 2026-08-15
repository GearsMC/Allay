package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.server.entity.ai.route.posevaluator.SpacePosEvaluator;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Uc boyutlu rastgele dolasma; {@link FlatRandomRoamExecutor}'un yukseklik de secen hali.
 *
 * <p>Duz dolasma yalnizca X ve Z seciyor ve Y'yi varligin bulundugu seviyede birakiyor. Bir balik
 * ya da blaze icin bu, mobun girdigi derinlikte sonsuza kadar sikismasi demek. Burada hedef uc
 * eksende birden seciliyor.</p>
 *
 * <p>Secilen nokta, varligin gercekten bulunabilecegi bir yer olana kadar yeniden deneniyor. Bu
 * kontrol icin rotayi kuran yol bulucunun konum degerlendiricisinin ayni ornegi disaridan
 * veriliyor; boylece dolasma hedefi ile rotanin gectigi yerler ayni kurala uyuyor — suyun disi bir
 * balik icin, katinin ici bir blaze icin gecersizdir.</p>
 */
public class SpaceRandomRoamExecutor implements BehaviorExecutor {

    protected static final double TARGET_REACHED_DISTANCE_SQUARED = 1.5;

    /** Hedefe hic yaklasamadan gecen bu kadar tick sonra hedef birakilir. */
    protected static final int MAX_STUCK_TICKS = 60;

    protected final float speed;
    protected final int maxRoamRange;
    protected final int maxVerticalRange;
    protected final int frequency;
    protected final int maxRetryTime;
    protected final SpacePosEvaluator posEvaluator;

    protected int targetCalTick;
    protected int stuckTick;
    protected boolean hasTarget;

    /**
     * Bir uc boyutlu dolasma executor'u olusturur.
     *
     * @param speed dolasma hizi
     * @param maxRoamRange en fazla yatay dolasma mesafesi (blok)
     * @param maxVerticalRange en fazla dikey dolasma mesafesi (blok)
     * @param frequency yeni hedef secimleri arasindaki bekleme (tick)
     * @param maxRetryTime bir hedef secerken kac rastgele nokta denenecegi
     * @param posEvaluator hedefin gecerli olup olmadigina karar veren degerlendirici; rotayi kuran
     *                     yol bulucuya verilenin ayni olmali
     */
    public SpaceRandomRoamExecutor(float speed, int maxRoamRange, int maxVerticalRange,
                                   int frequency, int maxRetryTime, SpacePosEvaluator posEvaluator) {
        this.speed = speed;
        this.maxRoamRange = maxRoamRange;
        this.maxVerticalRange = maxVerticalRange;
        this.frequency = frequency;
        this.maxRetryTime = maxRetryTime;
        this.posEvaluator = posEvaluator;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        targetCalTick = 0;
        stuckTick = 0;
        hasTarget = false;
        entity.setMovementSpeed(speed);
        entity.setPitchEnabled(true);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        targetCalTick++;

        var moveTarget = entity.getMoveTarget();
        if (moveTarget != null && hasTarget) {
            if (entity.getLocation().distanceSquared(moveTarget) < TARGET_REACHED_DISTANCE_SQUARED
                || ++stuckTick >= MAX_STUCK_TICKS) {
                abandonTarget(entity);
            }
        }

        if (!hasTarget && targetCalTick >= frequency) {
            findNewTarget(entity);
        }

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        abandonTarget(entity);
        entity.setPitchEnabled(false);
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    protected void abandonTarget(EntityIntelligent entity) {
        hasTarget = false;
        stuckTick = 0;
        targetCalTick = 0;
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
    }

    protected void findNewTarget(EntityIntelligent entity) {
        var rand = ThreadLocalRandom.current();
        var loc = entity.getLocation();

        for (int attempt = 0; attempt < maxRetryTime; attempt++) {
            var target = new Vector3d(
                    loc.x() + rand.nextInt(-maxRoamRange, maxRoamRange + 1),
                    loc.y() + rand.nextInt(-maxVerticalRange, maxVerticalRange + 1),
                    loc.z() + rand.nextInt(-maxRoamRange, maxRoamRange + 1)
            );

            if (posEvaluator != null && !posEvaluator.evaluate(entity, target)) {
                continue;
            }

            hasTarget = true;
            stuckTick = 0;
            targetCalTick = 0;
            EntityControlHelper.setRouteTarget(entity, target);
            EntityControlHelper.setLookTarget(entity, target);
            return;
        }
    }
}
