package org.allaymc.api.entity.component;

import org.allaymc.api.entity.data.WeaponStance;

/**
 * Shared component for mobs that visibly wind up a weapon before attacking.
 *
 * <p>The stance is server state that only exists to reach the client: it decides which of the
 * bow/crossbow animations plays. Implementations broadcast entity state when it changes.</p>
 *
 * @author daoge_cmd
 */
public interface EntityWeaponStanceComponent extends EntityBaseComponent {

    /**
     * @return the phase of weapon use this mob is currently in.
     */
    WeaponStance getWeaponStance();

    /**
     * Set the phase of weapon use. Setting the value it already has does nothing.
     *
     * @param stance the new stance
     */
    void setWeaponStance(WeaponStance stance);

    /**
     * @return whether this mob currently has something it wants to attack. Drives the aggressive
     * pose, and on illagers it is what makes them raise their weapon at all.
     */
    boolean isAggressive();

    /**
     * Set whether this mob is going after a target.
     *
     * @param aggressive whether the mob has a target
     */
    void setAggressive(boolean aggressive);
}
