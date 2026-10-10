package org.allaymc.server.player;

import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.player.Player;

public final class PlayerMovementSpeed {

    public static final double SPRINT_FACTOR = 1.3;
    public static final double SPEED_PER_LEVEL = 0.2;
    public static final double SLOWNESS_PER_LEVEL = 0.15;

    private PlayerMovementSpeed() {
    }

    public static double calculateMultiplier(boolean sprinting, int speedLevel, int slownessLevel) {
        var multiplier = sprinting ? SPRINT_FACTOR : 1.0;
        multiplier *= 1 + SPEED_PER_LEVEL * speedLevel;
        multiplier *= Math.max(0, 1 - SLOWNESS_PER_LEVEL * slownessLevel);
        return multiplier;
    }

    public static Player.Speed calculate(EntityPlayer entity, Player.Speed current) {
        var multiplier = calculateMultiplier(
                entity.isSprinting(),
                entity.getEffectLevel(EffectTypes.SPEED),
                entity.getEffectLevel(EffectTypes.SLOWNESS)
        );
        return new Player.Speed(current.baseSpeed(), multiplier);
    }

    public static void update(EntityPlayer entity) {
        if (!entity.isActualPlayer()) {
            return;
        }

        var controller = entity.getController();
        controller.setSpeed(calculate(entity, controller.getSpeed()));
    }
}
