package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;

/**
 * Sulfur kupunun amacsiz dolasmasi; blok emdigi anda duran hali.
 *
 * <p>Ayri bir sinif olmasinin sebebi davranis grubunun calisma sekli: bir davranisin evaluator'u
 * yalnizca <em>baslarken</em> kontrol ediliyor, calisirken degil. Dolasma davranisi suresiz
 * oldugundan bir kez basladiginda kendiliginden hic bitmiyor; kup blogu emse bile dolasmaya devam
 * ediyordu. Durmasi icin kosulun evaluator'da degil, calisan executor'un icinde olmasi gerek.</p>
 *
 * <p>Wiki: "When a sulfur cube has absorbed a block, it stops moving."</p>
 */
public class SulfurCubeRoamExecutor extends FlatRandomRoamExecutor {

    public SulfurCubeRoamExecutor(float speed, int maxRoamRange, int frequency,
                                  boolean calNextTargetImmediately, int runningTime,
                                  boolean avoidWater, int maxRetryTime) {
        super(speed, maxRoamRange, frequency, calNextTargetImmediately, runningTime, avoidWater, maxRetryTime);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        if (entity instanceof EntitySulfurCubeBaseComponent cube && cube.getAbsorbedBlock() != null) {
            return false;
        }

        return super.execute(entity);
    }
}
