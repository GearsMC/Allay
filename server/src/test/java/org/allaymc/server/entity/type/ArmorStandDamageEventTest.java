package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.EntityState;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityArmorStand;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.entity.EntityDamageEvent;
import org.allaymc.api.server.Server;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Zirh standinin iki vurusta kirilma yolu da {@link EntityDamageEvent}'ten gecmeli; eklentiler
 * hologram olarak kullandiklari standin kirilmasini ancak bu olayla engelleyebilir.
 */
@ExtendWith(AllayTestExtension.class)
class ArmorStandDamageEventTest {

    private static EntityArmorStand stand() {
        var stand = EntityTypes.ARMOR_STAND.createEntity(EntityInitInfo.builder()
                .dimension(Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension())
                .pos(0, 64, 0).build());
        var base = (EntityBaseComponentImpl) ((EntityImpl) stand).getBaseComponent();
        base.setState(EntityState.SPAWNED_LATER);
        base.setState(EntityState.ALIVE);
        return stand;
    }

    public static final class Listener {
        final EntityArmorStand stand;
        final boolean cancel;
        final AtomicInteger calls = new AtomicInteger();

        Listener(EntityArmorStand stand, boolean cancel) {
            this.stand = stand;
            this.cancel = cancel;
        }

        @EventHandler
        public void damage(EntityDamageEvent event) {
            if (event.getEntity() != stand) {
                return;
            }
            calls.incrementAndGet();
            if (cancel) {
                event.cancel();
            }
        }
    }

    @Test
    void cancelledHitsNeverBreakTheStand() {
        var stand = stand();
        var listener = new Listener(stand, true);
        var bus = Server.getInstance().getEventBus();
        bus.registerListener(listener);
        try {
            assertFalse(stand.attack(DamageContainer.simpleAttack(1)));
            assertFalse(stand.attack(DamageContainer.simpleAttack(1)));
        } finally {
            bus.unregisterListener(listener);
        }
        assertEquals(2, listener.calls.get());
        assertTrue(stand.isAlive(), "iptal edilen vuruslar standi kirmamali");
    }

    @Test
    void hitIsVisibleToListeners() {
        var stand = stand();
        var listener = new Listener(stand, false);
        var bus = Server.getInstance().getEventBus();
        bus.registerListener(listener);
        try {
            stand.attack(DamageContainer.simpleAttack(1));
        } finally {
            bus.unregisterListener(listener);
        }
        assertEquals(1, listener.calls.get());
    }
}
