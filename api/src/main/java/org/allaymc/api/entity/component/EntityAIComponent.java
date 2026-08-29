package org.allaymc.api.entity.component;

import org.allaymc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.allaymc.api.entity.ai.memory.MemoryStorage;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.joml.Vector3dc;

/**
 * Component for AI-controlled entities that expose a behavior group and shared AI memory.
 *
 * @author daoge_cmd
 */
public interface EntityAIComponent extends EntityComponent {

    /**
     * Get the current behavior group.
     *
     * @return the behavior group
     */
    BehaviorGroup getBehaviorGroup();

    /**
     * Set the behavior group for this entity.
     *
     * @param behaviorGroup the behavior group
     */
    void setBehaviorGroup(BehaviorGroup behaviorGroup);

    /**
     * Check whether this entity is currently controlled through the manual navigation API.
     *
     * @return {@code true} when autonomous sensors and behaviors are paused
     */
    boolean isManualControlEnabled();

    /**
     * Enable or disable manual plugin control for this entity.
     * <p>
     * Enabling manual control interrupts autonomous behaviors on the next AI tick while
     * keeping route finding and controllers active. Disabling it clears manual targets and
     * resumes autonomous behavior evaluation.
     *
     * @param enabled whether manual control should be enabled
     */
    void setManualControlEnabled(boolean enabled);

    /**
     * Navigate to a position while manual control is enabled.
     *
     * @param target the world position to navigate to
     * @param speed the movement speed, which must be finite and greater than zero
     * @throws IllegalStateException if manual control is not enabled
     * @throws IllegalArgumentException if the target or speed is not finite, or the speed is not positive
     * @throws NullPointerException if the target is {@code null}
     */
    void navigateTo(Vector3dc target, float speed);

    /**
     * Stop navigation requested through manual control.
     */
    void stopNavigation();

    /**
     * Look at a position while manual control is enabled.
     *
     * @param target the world position to look at
     * @throws IllegalStateException if manual control is not enabled
     * @throws IllegalArgumentException if the target is not finite
     * @throws NullPointerException if the target is {@code null}
     */
    void lookAt(Vector3dc target);

    /**
     * Stop looking at a target requested through manual control.
     */
    void stopLooking();

    /**
     * Get the memory storage of the current behavior group.
     *
     * @return the memory storage
     */
    default MemoryStorage getMemoryStorage() {
        return getBehaviorGroup().getMemoryStorage();
    }

    /**
     * Get the movement speed of this entity.
     *
     * @return the movement speed
     */
    default float getMovementSpeed() {
        return getMemoryStorage().get(MemoryTypes.MOVEMENT_SPEED);
    }

    /**
     * Set the movement speed of this entity.
     *
     * @param speed the movement speed
     */
    default void setMovementSpeed(float speed) {
        getMemoryStorage().put(MemoryTypes.MOVEMENT_SPEED, speed);
    }

    default Vector3dc getLookTarget() {
        return getMemoryStorage().get(MemoryTypes.LOOK_TARGET);
    }

    default void setLookTarget(Vector3dc target) {
        getMemoryStorage().put(MemoryTypes.LOOK_TARGET, target);
    }

    default Vector3dc getMoveTarget() {
        return getMemoryStorage().get(MemoryTypes.MOVE_TARGET);
    }

    default void setMoveTarget(Vector3dc target) {
        getMemoryStorage().put(MemoryTypes.MOVE_TARGET, target);
    }

    default Vector3dc getMoveDirectionStart() {
        return getMemoryStorage().get(MemoryTypes.MOVE_DIRECTION_START);
    }

    default void setMoveDirectionStart(Vector3dc start) {
        getMemoryStorage().put(MemoryTypes.MOVE_DIRECTION_START, start);
    }

    default Vector3dc getMoveDirectionEnd() {
        return getMemoryStorage().get(MemoryTypes.MOVE_DIRECTION_END);
    }

    default void setMoveDirectionEnd(Vector3dc end) {
        getMemoryStorage().put(MemoryTypes.MOVE_DIRECTION_END, end);
    }

    default boolean hasMoveDirection() {
        return getMoveDirectionStart() != null && getMoveDirectionEnd() != null;
    }

    default boolean shouldUpdateMoveDirection() {
        return getMemoryStorage().get(MemoryTypes.SHOULD_UPDATE_MOVE_DIRECTION);
    }

    default void setShouldUpdateMoveDirection(boolean shouldUpdate) {
        getMemoryStorage().put(MemoryTypes.SHOULD_UPDATE_MOVE_DIRECTION, shouldUpdate);
    }

    default boolean isPitchEnabled() {
        return getMemoryStorage().get(MemoryTypes.ENABLE_PITCH);
    }

    default void setPitchEnabled(boolean enabled) {
        getMemoryStorage().put(MemoryTypes.ENABLE_PITCH, enabled);
    }
}
