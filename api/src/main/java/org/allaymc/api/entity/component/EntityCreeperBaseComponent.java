package org.allaymc.api.entity.component;

/**
 * Shared component for creeper entities that track whether the fuse is burning.
 *
 * @author daoge_cmd
 */
public interface EntityCreeperBaseComponent extends EntityBaseComponent {

    /**
     * Check if this creeper is currently swelling up to explode.
     *
     * @return {@code true} if the fuse is burning
     */
    boolean isSwelling();

    /**
     * Set whether this creeper's fuse is burning. Drives the swelling animation on the client.
     *
     * @param swelling {@code true} to start the fuse
     */
    void setSwelling(boolean swelling);
}
