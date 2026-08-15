package org.allaymc.server.entity.component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Kurt icin canli varlik bileseni.
 *
 * <p>Vahsi kurtlar esya dusurmez; bu yuzden saldirgan varsayilandan yalnizca can ve deneyim
 * degerleri farklidir.</p>
 */
public class EntityWolfLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityWolfLivingComponentImpl() {
        setMaxHealth(8);
    }

    @Override
    public int getDropXpAmount() {
        return ThreadLocalRandom.current().nextInt(1, 4);
    }
}
