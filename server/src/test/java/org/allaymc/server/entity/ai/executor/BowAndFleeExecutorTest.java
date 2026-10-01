package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.memory.MemoryStorage;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.manager.EntityManager;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.joml.Vector3dc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BowAndFleeExecutorTest {

    @Test
    void bowCycleFollowsDistanceAndDrawTime() {
        BowAttackExecutor executor = new BowAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.12f, 40, false);
        for (int i = 0; i < 500; i++) {
            // Yakin: aralik 20 tick, germe 20-30 => toplam 20-30, nisan suresi 10 dusulur.
            int near = executor.rollCharge(0.0);
            assertTrue(near >= 10 && near <= 20, "yakin atis: " + near);
            // Menzil siniri: aralik 60 tick => toplam 60, nisan dusulunce 50.
            assertEquals(50, executor.rollCharge(15.0));
            // Menzilin otesi de 60'ta sinirlanir.
            assertEquals(50, executor.rollCharge(40.0));
        }
    }

    @Test
    void bowVelocityMatchesHeartCore() {
        assertEquals(1.15, BowAttackExecutor.velocity(0), 1e-9);
        assertEquals(1.4, BowAttackExecutor.velocity(10), 1e-9);
        assertEquals(1.7, BowAttackExecutor.velocity(50), 1e-9);
    }

    @Test
    void bowAimCompensatesDropAndKeepsDirection() {
        BowAttackExecutor executor = new BowAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.12f, 40, false);
        Vector3d direction = new Vector3d(10, 0, 0);
        executor.adjustDirection(direction, 10);
        // Yukari telafi: 0.03 * 10 = 0.3; sapma mesafeyle sinirli, yani yon hala +x.
        direction.normalize();
        assertTrue(direction.x > 0.9, "yon korunmali: " + direction);
        assertTrue(direction.y > 0.0, "dusus telafisi yukari olmali: " + direction);
    }

    @Test
    void fleeMovesAwayFromThreatAndLingersAfterItDisappears() {
        Dimension dimension = mock(Dimension.class);
        EntityManager entities = mock(EntityManager.class);
        when(dimension.getEntityManager()).thenReturn(entities);
        MemoryStorage memory = mock(MemoryStorage.class);
        EntityIntelligent rabbit = mock(EntityIntelligent.class);
        when(rabbit.getMemoryStorage()).thenReturn(memory);
        when(rabbit.getDimension()).thenReturn(dimension);
        when(rabbit.getLocation()).thenReturn(new Location3d(0, 64, 0, 0, 0, dimension));
        when(rabbit.getMovementSpeed()).thenReturn(0.22f);
        when(rabbit.getBehaviorGroup()).thenReturn(mock(org.allaymc.api.entity.ai.behaviorgroup.BehaviorGroup.class));

        EntityPlayer player = mock(EntityPlayer.class);
        when(player.isAlive()).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location3d(3, 64, 0, 0, 0, dimension));
        when(memory.get(MemoryTypes.NEAREST_PLAYER)).thenReturn(5L);
        when(entities.getEntity(eq(5L))).thenReturn(player);

        FleeFromTargetExecutor executor = new FleeFromTargetExecutor(MemoryTypes.NEAREST_PLAYER, 0.22f, 4.5, 60);
        executor.onStart(rabbit);
        assertTrue(executor.execute(rabbit));

        ArgumentCaptor<Vector3dc> target = ArgumentCaptor.forClass(Vector3dc.class);
        verify(rabbit).setMoveTarget(target.capture());
        // Oyuncu +x tarafinda; kacis hedefi -x'te, 4.5 blok ileride.
        assertEquals(-4.5, target.getValue().x(), 1e-9);

        // Oyuncu kayboldu: 60 tick boyunca son bilinen konumdan kacmaya devam eder, sonra durur.
        when(memory.get(MemoryTypes.NEAREST_PLAYER)).thenReturn(null);
        for (int i = 0; i < 59; i++) {
            assertTrue(executor.execute(rabbit), "tick " + i);
        }
        assertFalse(executor.execute(rabbit));
    }
}
