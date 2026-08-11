package org.allaymc.api.entity.data;

/**
 * Where a mob is in the cycle of using a ranged weapon.
 *
 * <p>The client plays the draw and hold animations itself, but only when it is told which phase the
 * mob is in — without this a skeleton stands with its bow at its side while arrows fly out of it.</p>
 */
public enum WeaponStance {
    /**
     * Not using the weapon. Arms down.
     */
    IDLE,
    /**
     * Drawing a bow or cranking a crossbow. The client plays the pulling animation.
     */
    CHARGING,
    /**
     * Weapon fully drawn or loaded, held on target and about to fire.
     */
    READY
}
