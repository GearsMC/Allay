package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Teleports the entity to a random safe spot nearby, the way an enderman escapes when hurt.
 *
 * <p>A number of random offsets are tried and the first one the entity can actually stand on is
 * used; if none of them work the entity simply stays put and the behavior ends, letting a
 * lower-priority behavior take over on the next tick. The whole thing runs in a single tick — this
 * executor never returns {@code true}, so it never holds the behavior slot.</p>
 */
public class TeleportAwayExecutor implements BehaviorExecutor {

    protected final int horizontalRange;
    protected final int verticalRange;
    protected final int maxAttempts;

    /**
     * Creates a teleport executor with vanilla-like enderman range.
     */
    public TeleportAwayExecutor() {
        this(32, 16, 16);
    }

    /**
     * Creates a teleport executor.
     *
     * @param horizontalRange the maximum horizontal distance to teleport, in blocks.
     * @param verticalRange the maximum vertical distance to teleport, in blocks.
     * @param maxAttempts how many random positions are tried before giving up.
     */
    public TeleportAwayExecutor(int horizontalRange, int verticalRange, int maxAttempts) {
        this.horizontalRange = horizontalRange;
        this.verticalRange = verticalRange;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var destination = findDestination(entity);
        if (destination == null) {
            return false;
        }

        var dimension = entity.getDimension();
        var from = entity.getLocation();
        // Particles at both ends, otherwise the mob looks like it simply vanished.
        dimension.addParticle(from.x(), from.y() + entity.getEyeHeight(), from.z(), SimpleParticle.ENDERMAN_TELEPORT);
        dimension.addSound(new Vector3d(from.x(), from.y(), from.z()), SimpleSound.TELEPORT);

        entity.teleport(destination);

        dimension.addParticle(destination.x(), destination.y() + entity.getEyeHeight(), destination.z(), SimpleParticle.ENDERMAN_TELEPORT);
        dimension.addSound(new Vector3d(destination.x(), destination.y(), destination.z()), SimpleSound.TELEPORT);

        // Whatever route was being followed points at the old position now.
        EntityControlHelper.removeRouteTarget(entity);
        return false;
    }

    /**
     * Picks a random nearby position the entity can stand on, or {@code null} when every attempt
     * landed somewhere unusable.
     */
    protected Location3d findDestination(EntityIntelligent entity) {
        var rand = ThreadLocalRandom.current();
        var dimension = entity.getDimension();
        var location = entity.getLocation();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double x = location.x() + rand.nextInt(-horizontalRange, horizontalRange + 1);
            double z = location.z() + rand.nextInt(-horizontalRange, horizontalRange + 1);
            double y = location.y() + rand.nextInt(-verticalRange, verticalRange + 1);

            if (entity.canStandSafely(x, y, z, dimension)) {
                return new Location3d(x, y, z, location.pitch(), location.yaw(), dimension);
            }
        }

        return null;
    }
}
