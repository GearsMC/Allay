package org.allaymc.server.entity.component;

/**
 * Physics for mobs that hover under their own power, such as the blaze.
 *
 * <p>Gravity is switched off entirely — a hovering mob holds its altitude from the route it is
 * following, not from a balance of lift and weight. Air drag is raised well above the default so
 * that motion bleeds off quickly once {@code FlyController} stops pushing; with the default drag
 * the mob would coast far past its waypoint and visibly wobble.</p>
 */
public class EntityFlyingPhysicsComponentImpl extends EntityPhysicsComponentImpl {

    @Override
    public double getGravity() {
        return 0;
    }

    @Override
    public double getDragFactorInAir() {
        return 0.09;
    }

    @Override
    public boolean computeLiquidPhysics() {
        // A flying mob should not bob around in liquid; its route already decides where it goes.
        return false;
    }
}
