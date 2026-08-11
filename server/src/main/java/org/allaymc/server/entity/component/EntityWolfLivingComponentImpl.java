package org.allaymc.server.entity.component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Living component implementation for wolves.
 *
 * <p>Wild wolves drop nothing, so only the health and experience values differ from the
 * hostile default.</p>
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
