package org.allaymc.server.entity.ai.controller;

import org.allaymc.api.entity.ai.controller.Controller;
import org.allaymc.api.entity.interfaces.EntityIntelligent;

/**
 * Free-flight movement controller, the airborne counterpart of {@link WalkController}.
 *
 * <p>Where the walk controller only drives horizontal motion and jumps over obstacles, this one
 * steers straight at the waypoint in all three axes. It is meant to be paired with a route finder
 * that produces 3D routes and with physics that apply no gravity — otherwise the entity would sink
 * between waypoints.</p>
 */
public class FlyController implements Controller {

    /**
     * Squared-speed multiplier above which external motion (knockback, explosions) is left alone
     * instead of being overwritten. Mirrors the threshold {@link WalkController} uses.
     */
    protected static final double EXTERNAL_MOTION_THRESHOLD = 0.4756;

    @Override
    public boolean control(EntityIntelligent entity) {
        if (!entity.hasMoveDirection()) {
            return false;
        }

        var end = entity.getMoveDirectionEnd();
        if (end == null) {
            return false;
        }

        var motion = entity.getMotion();
        float speed = entity.getMovementSpeed();
        // Let knockback play out rather than fighting it.
        if (motion.lengthSquared() > speed * speed * EXTERNAL_MOTION_THRESHOLD) {
            return false;
        }

        var loc = entity.getLocation();
        double dx = end.x() - loc.x();
        double dy = end.y() - loc.y();
        double dz = end.z() - loc.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Already there — updateRoute() advances to the next waypoint next tick.
        if (distance < 0.01) {
            return false;
        }

        // Clamp near the waypoint so the entity settles instead of oscillating past it.
        double factor = Math.min(speed, distance) / distance;
        entity.addMotion(
                dx * factor - motion.x(),
                dy * factor - motion.y(),
                dz * factor - motion.z()
        );

        return true;
    }
}
