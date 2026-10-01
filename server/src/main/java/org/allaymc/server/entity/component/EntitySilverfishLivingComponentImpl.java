package org.allaymc.server.entity.component;

/**
 * Gumus balik icin canli varlik bileseni. Esya dusurmez, yalnizca deneyim birakir.
 */
public class EntitySilverfishLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntitySilverfishLivingComponentImpl() {
        setMaxHealth(8);
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
