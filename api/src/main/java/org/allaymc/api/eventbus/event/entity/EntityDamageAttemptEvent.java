package org.allaymc.api.eventbus.event.entity;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.eventbus.event.CancellableEvent;

/**
 * Configures a damage attempt before the cooldown check. Cancellation prevents all damage effects.
 * This event does not indicate a successful hit; use {@link EntityAfterDamageEvent} for that.
 * The victim's last damage still describes the previous accepted hit.
 */
@Getter
@CallerThread(ThreadType.WORLD)
@CallerThread(ThreadType.DIMENSION)
public class EntityDamageAttemptEvent extends EntityEvent implements CancellableEvent {
    private final DamageContainer damageContainer;
    /** Whether this attempt may pass the victim's existing damage cooldown. */
    @Setter
    private boolean ignoreCoolDown;

    /** Creates an attempt with the cooldown policy requested by the attack caller. */
    public EntityDamageAttemptEvent(Entity entity, DamageContainer damageContainer, boolean ignoreCoolDown) {
        super(entity);
        this.damageContainer = damageContainer;
        this.ignoreCoolDown = ignoreCoolDown;
    }
}
