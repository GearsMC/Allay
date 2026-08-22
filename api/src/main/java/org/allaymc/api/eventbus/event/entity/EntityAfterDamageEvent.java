package org.allaymc.api.eventbus.event.entity;

import lombok.Getter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.damage.DamageContainer;

/**
 * Called right after damage has been applied to an entity.
 * <p>
 * Unlike {@link EntityDamageEvent}, which is called before armour, effects and enchantment
 * modifiers run, this event is called once the damage is final: {@link DamageContainer#finalDamage}
 * is the amount of health the entity actually lost, and the entity's health already reflects it.
 * Listeners that need the real damage dealt — damage leaderboards, life steal, thorns-like
 * effects — belong here.
 * <p>
 * The event is not cancellable: the damage has already happened by the time it is called.
 *
 * @author GearsMC
 */
@Getter
@CallerThread(ThreadType.WORLD)
@CallerThread(ThreadType.DIMENSION)
public class EntityAfterDamageEvent extends EntityEvent {

    /**
     * The damage data for this event, with every modifier already applied.
     */
    protected final DamageContainer damageContainer;

    public EntityAfterDamageEvent(Entity entity, DamageContainer damageContainer) {
        super(entity);
        this.damageContainer = damageContainer;
    }
}
