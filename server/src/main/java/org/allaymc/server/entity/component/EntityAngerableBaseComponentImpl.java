package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

/**
 * Base component for mobs whose client-side model changes while they are hunting something.
 *
 * <p>The angry look is driven by an entity flag, and flags only reach the client when entity state
 * is broadcast. Nothing broadcasts on its own when a target is acquired or lost, so this component
 * watches the target memory and pushes an update on the tick it flips — checking a memory slot
 * every tick is cheap, re-sending metadata every tick would not be.</p>
 */
public abstract class EntityAngerableBaseComponentImpl extends EntityBaseComponentImpl {

    @Dependency
    protected EntityAIComponent aiComponent;

    protected boolean angry;

    protected EntityAngerableBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    /**
     * Tells whether a mob is currently after something — either it was provoked, or a player walked
     * into its sight range. Shared with the metadata writer so the flag on the wire and the flag
     * that triggers the broadcast can never disagree.
     *
     * @param entity the mob to inspect.
     * @return whether the mob should be shown in its angry pose.
     */
    public static boolean isHunting(EntityIntelligent entity) {
        var memory = entity.getMemoryStorage();
        return memory.get(MemoryTypes.ATTACK_TARGET) != null || memory.get(MemoryTypes.NEAREST_PLAYER) != null;
    }

    /**
     * @return whether the mob currently has something it wants to attack.
     */
    public boolean isAngry() {
        return angry;
    }

    @EventHandler
    protected void onAngerTick(CEntityTickEvent event) {
        var hunting = isHunting((EntityIntelligent) thisEntity);
        if (hunting == angry) {
            return;
        }

        angry = hunting;
        broadcastState();
    }
}
