package org.allaymc.server.entity;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.manager.EntityManager;
import org.allaymc.server.scheduler.AllayScheduler;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Boyut değiştiren varlık geçiş boyunca geçerli sayılır. Eskiden ışınlanma varlığın kendi zamanlayıcısındaki
 * bir görevden yapılınca durum hemen DESPAWNED_LATER oluyor, zamanlayıcı aynı tick'te sıradaki görevleri
 * (ör. pet takibi) "yaratıcı geçersiz" diye kalıcı olarak iptal ediyordu.
 */
@ExtendWith(AllayTestExtension.class)
class DimensionChangeValidityTest {

    private static Entity spawnPig() throws InterruptedException {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        var pig = EntityTypes.PIG.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(0, 100, 0)
                .build());
        pig.setPersistent(false);
        dimension.getEntityManager().addEntity(pig);
        for (int i = 0; i < 200 && !pig.isSpawned(); i++) {
            Thread.sleep(10);
        }
        assertTrue(pig.isSpawned(), "test varlığı doğmadı");
        return pig;
    }

    /** Eklemeyi hiç tamamlamayan hedef boyut: varlık geçişte kalır. */
    private static Dimension pendingTargetDimension() {
        var dimension = mock(Dimension.class);
        when(dimension.getEntityManager()).thenReturn(mock(EntityManager.class));
        return dimension;
    }

    @Test
    void entityStaysValidWhileChangingDimension() throws InterruptedException {
        var pig = spawnPig();

        assertTrue(pig.teleport(new Location3d(0, 64, 0, pendingTargetDimension())));

        assertFalse(pig.isSpawned());
        assertTrue(pig.isValid(), "geçişteki varlık geçersiz sayılmamalı");
    }

    @Test
    void removedEntityIsInvalid() throws InterruptedException {
        var pig = spawnPig();
        pig.remove();
        assertFalse(pig.isValid());
    }

    @Test
    void repeatingTaskSurvivesTeleportFromSameTick() throws InterruptedException {
        var pig = spawnPig();
        var scheduler = new AllayScheduler(Executors.newVirtualThreadPerTaskExecutor());
        var runs = new AtomicInteger();

        // Aynı tick'te önce ışınlama görevi, sonra tekrarlanan görev çalışır (pet takibi + /spawn).
        scheduler.runLater(pig, () -> pig.teleport(new Location3d(0, 64, 0, pendingTargetDimension())));
        scheduler.scheduleRepeating(pig, () -> {
            runs.incrementAndGet();
            return true;
        }, 1);

        scheduler.tick();
        scheduler.tick();
        scheduler.tick();

        assertFalse(pig.isSpawned());
        assertTrue(runs.get() >= 2, "tekrarlanan görev ışınlanmadan sonra iptal edildi: " + runs.get());
    }
}
