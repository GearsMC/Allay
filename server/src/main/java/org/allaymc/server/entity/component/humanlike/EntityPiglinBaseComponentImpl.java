package org.allaymc.server.entity.component.humanlike;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.item.type.ItemTypes;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Piglin base behavior: hitbox, weapon variant and combat animation state.
 *
 * <p>A piglin spawns holding either a crossbow or a golden sword, split evenly. The choice drives
 * the behavior group too — the crossbow variant shoots from a distance while the sword variant
 * charges in, both selected at runtime by reading the hand slot.</p>
 *
 * <p>Everything else — only arming an empty hand so a stored weapon survives a restart, and
 * carrying the stance the client needs to animate the crossbow — comes from
 * {@link EntityArmedBaseComponentImpl}, which the other armed mobs use as well.</p>
 */
public class EntityPiglinBaseComponentImpl extends EntityArmedBaseComponentImpl {

    public EntityPiglinBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, () -> ThreadLocalRandom.current().nextBoolean()
                ? ItemTypes.CROSSBOW
                : ItemTypes.GOLDEN_SWORD, 0.6, 1.95);
    }
}
