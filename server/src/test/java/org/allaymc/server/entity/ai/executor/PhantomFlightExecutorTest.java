package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryStorage;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.manager.EntityManager;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fantom ucus dongusunu motor olmadan benzetir: konum her tick ivmeyle ilerletilir.
 */
class PhantomFlightExecutorTest {

    private static final long TARGET_ID = 7L;

    /** Tek tick'lik basit fizik: konum += ivme. */
    private static final class Sim {
        double x;
        double y;
        double z;
        double mx;
        double my;
        double mz;
        double maxDistance;
        double minDistance = Double.MAX_VALUE;
    }

    private EntityIntelligent phantom(Sim sim, Dimension dimension, EntityPlayer target) {
        EntityIntelligent phantom = mock(EntityIntelligent.class);
        MemoryStorage memory = mock(MemoryStorage.class);
        when(memory.get(MemoryTypes.ATTACK_TARGET)).thenReturn(TARGET_ID);
        when(phantom.getMemoryStorage()).thenReturn(memory);
        when(phantom.getDimension()).thenReturn(dimension);
        when(phantom.getLocation()).thenAnswer(inv -> new Location3d(sim.x, sim.y, sim.z, 0, 0, dimension));
        when(phantom.getMotion()).thenAnswer(inv -> new Vector3d(sim.mx, sim.my, sim.mz));
        doAnswer(inv -> {
            sim.mx = inv.getArgument(0);
            sim.my = inv.getArgument(1);
            sim.mz = inv.getArgument(2);
            return null;
        }).when(phantom).setMotion(anyDouble(), anyDouble(), anyDouble());
        return phantom;
    }

    @Test
    void circlesThenDivesAndHitsTarget() {
        Dimension dimension = mock(Dimension.class);
        EntityManager entities = mock(EntityManager.class);
        when(dimension.getEntityManager()).thenReturn(entities);

        EntityPlayer target = mock(EntityPlayer.class);
        when(entities.getEntity(eq(TARGET_ID))).thenReturn(target);
        when(target.isAlive()).thenReturn(true);
        when(target.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(target.getEyeHeight()).thenReturn(1.62d);
        // Oyuncu +x yonune bakiyor; fantom bir sure bakis konisinin disina cikacak.
        when(target.getLocation()).thenAnswer(inv -> new Location3d(0, 64, 0, -90, 0, dimension));
        AtomicInteger hits = new AtomicInteger();
        when(target.attack(any(DamageContainer.class))).thenAnswer(inv -> {
            hits.incrementAndGet();
            return true;
        });

        Sim sim = new Sim();
        sim.x = 12;
        sim.y = 80;
        sim.z = 12;
        EntityIntelligent phantom = phantom(sim, dimension, target);

        BehaviorExecutor executor = new PhantomFlightExecutor(MemoryTypes.ATTACK_TARGET, 48, false);
        executor.onStart(phantom);
        for (int tick = 0; tick < 3000; tick++) {
            assertTrue(executor.execute(phantom), "hedef varken davranis surmeli, tick " + tick);
            sim.x += sim.mx;
            sim.y += sim.my;
            sim.z += sim.mz;
            double distance = Math.sqrt(sim.x * sim.x + (sim.y - 64) * (sim.y - 64) + sim.z * sim.z);
            sim.maxDistance = Math.max(sim.maxDistance, distance);
            sim.minDistance = Math.min(sim.minDistance, distance);
        }

        assertTrue(hits.get() >= 2, "fantom birden fazla dalis yapip vurmali, vurus: " + hits.get());
        assertTrue(sim.maxDistance < 48, "fantom takip menzilinin disina cikmamali: " + sim.maxDistance);
        assertTrue(sim.minDistance < 2.5, "dalista hedefe yaklasmali: " + sim.minDistance);
    }

    @Test
    void stopsWhenTargetIsGone() {
        Dimension dimension = mock(Dimension.class);
        EntityManager entities = mock(EntityManager.class);
        when(dimension.getEntityManager()).thenReturn(entities);
        when(entities.getEntity(eq(TARGET_ID))).thenReturn(null);

        Sim sim = new Sim();
        EntityIntelligent phantom = phantom(sim, dimension, null);
        BehaviorExecutor executor = new PhantomFlightExecutor(MemoryTypes.ATTACK_TARGET, 48, false);
        executor.onStart(phantom);
        assertEquals(false, executor.execute(phantom));
    }

    @Test
    void idleOrbitKeepsFlyingWithoutTarget() {
        Dimension dimension = mock(Dimension.class);
        Sim sim = new Sim();
        sim.y = 70;
        EntityIntelligent phantom = mock(EntityIntelligent.class);
        when(phantom.getDimension()).thenReturn(dimension);
        when(phantom.getLocation()).thenAnswer(inv -> new Location3d(sim.x, sim.y, sim.z, 0, 0, dimension));
        when(phantom.getMotion()).thenAnswer(inv -> new Vector3d(sim.mx, sim.my, sim.mz));
        doAnswer(inv -> {
            sim.mx = inv.getArgument(0);
            sim.my = inv.getArgument(1);
            sim.mz = inv.getArgument(2);
            return null;
        }).when(phantom).setMotion(anyDouble(), anyDouble(), anyDouble());

        BehaviorExecutor executor = new PhantomFlightExecutor(null, 0, false);
        executor.onStart(phantom);
        double maxRadius = 0;
        for (int tick = 0; tick < 2000; tick++) {
            assertTrue(executor.execute(phantom));
            sim.x += sim.mx;
            sim.y += sim.my;
            sim.z += sim.mz;
            maxRadius = Math.max(maxRadius, Math.sqrt(sim.x * sim.x + sim.z * sim.z));
        }
        assertTrue(maxRadius > 3, "bos yorunge hareket etmeli: " + maxRadius);
        assertTrue(maxRadius < 40, "bos yorunge baslangictan uzaklasmamali: " + maxRadius);
    }
}
