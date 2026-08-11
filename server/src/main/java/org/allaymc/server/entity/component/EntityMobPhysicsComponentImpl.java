package org.allaymc.server.entity.component;

/**
 * Physics component for walking mobs that are neither human-like nor breedable animals.
 *
 * <p>The only thing it changes is the step height: the default is {@code 0.0}, which would leave a
 * pathfinding mob stuck against the first slab or single block in its way.</p>
 */
public class EntityMobPhysicsComponentImpl extends EntityPhysicsComponentImpl {

    @Override
    public double getStepHeight() {
        return 0.6;
    }
}
