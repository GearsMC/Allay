package org.allaymc.server.entity.component;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.cloudburstmc.nbt.NbtType;

/**
 * @author daoge_cmd
 */
public class EntityHeadYawComponentImpl implements EntityHeadYawComponent {

    @Identifier.Component
    public static final Identifier IDENTIFIER = new Identifier("minecraft:entity_head_yaw_component");

    protected static final String TAG_ROTATION = "Rotation";

    @Getter
    @Setter
    protected double headYaw;

    /**
     * Starts the head aligned with the body.
     *
     * <p>The field defaults to {@code 0} and the spawn packet takes the head
     * rotation from this component whenever an entity has it, so an entity
     * spawned facing south was sent with its head facing north until something
     * moved it. Entities without this component fall back to the body yaw and
     * always looked right; this keeps the two consistent.</p>
     */
    @EventHandler
    protected void onLoadNBT(CEntityLoadNBTEvent event) {
        var nbt = event.getNbt();
        if (!nbt.containsKey(TAG_ROTATION)) {
            return;
        }
        var rotation = nbt.getList(TAG_ROTATION, NbtType.FLOAT);
        if (!rotation.isEmpty()) {
            this.headYaw = rotation.get(0);
        }
    }
}
