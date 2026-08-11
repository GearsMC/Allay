package org.allaymc.server.entity.component;

/**
 * Living component implementation for endermites.
 *
 * <p>Endermites drop no items, only experience.</p>
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
