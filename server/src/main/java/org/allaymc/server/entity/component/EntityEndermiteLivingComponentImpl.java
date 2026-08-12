package org.allaymc.server.entity.component;

/**
 * Endermite icin canli varlik bileseni.
 *
 * <p>Endermite esya dusurmez, yalnizca deneyim birakir.</p>
 */
public class EntityEndermiteLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityEndermiteLivingComponentImpl() {
        setMaxHealth(8);
    }

    @Override
    public int getDropXpAmount() {
        return 3;
    }
}
