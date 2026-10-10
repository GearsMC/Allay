package org.allaymc.server.entity.type;

import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityThrownTrident;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.allaymc.testutils.MobTestSite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.allaymc.testutils.MobTestSite.waitFor;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AllayTestExtension.class)
class DrownedTridentWorldTest {

    private MobTestSite site;

    @BeforeEach
    void setUp() {
        site = MobTestSite.create();
    }

    @AfterEach
    void tearDown() {
        site.cleanUp();
    }

    @Test
    void drownedThrowsTridentsThatCannotBeFarmed() {
        var drowned = site.spawn(EntityTypes.DROWNED, 0, 0, true);
        var pig = site.spawn(EntityTypes.PIG, 8, 0, true);
        drowned.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, pig.getRuntimeId());
        var executor = HostileMobEntityTypeInitializer.drownedTrident(MemoryTypes.ATTACK_TARGET, true);

        site.onWorldThread(() -> {
            executor.onStart(drowned);
            for (var i = 0; i < 60; i++) {
                executor.execute(drowned);
            }
        });

        assertTrue(waitFor(() -> site.findNear(EntityThrownTrident.class, 20) != null, 2000), "zipkin firlatilmadi");
        var trident = site.findNear(EntityThrownTrident.class, 20);
        site.track(trident);
        assertTrue(trident.getShooter() == drowned);
        assertNull(trident.getTridentItem(), "mob zipkini yerden toplanip gercek zipkina donusmemeli");
    }
}
