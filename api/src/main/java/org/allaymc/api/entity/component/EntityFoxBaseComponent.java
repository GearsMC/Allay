package org.allaymc.api.entity.component;

/**
 * Exposes the visual state specific to fox entities.
 */
public interface EntityFoxBaseComponent extends EntityBaseComponent {

    /**
     * Returns whether this fox is using its sleeping pose.
     *
     * @return {@code true} when the sleeping pose is active
     */
    boolean isSleeping();

    /**
     * Sets whether this fox uses its sleeping pose.
     * <p>
     * This state only controls fox metadata and does not interact with beds or world sleep rules.
     *
     * @param sleeping whether the sleeping pose should be active
     */
    void setSleeping(boolean sleeping);
}
