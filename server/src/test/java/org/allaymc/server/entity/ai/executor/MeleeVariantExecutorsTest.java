package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.Dimension;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeleeVariantExecutorsTest {

    private static EntityIntelligent spider(Dimension dimension, boolean onGround) {
        EntityIntelligent spider = mock(EntityIntelligent.class);
        when(spider.getLocation()).thenReturn(new Location3d(0, 64, 0, 0, 0, dimension));
        when(spider.isOnGround()).thenReturn(onGround);
        return spider;
    }

    private static EntityLiving target(Dimension dimension, double x, double z) {
        EntityLiving target = mock(EntityLiving.class);
        when(target.getLocation()).thenReturn(new Location3d(x, 64, z, 0, 0, dimension));
        return target;
    }

    @Test
    void spiderLeapsTowardTargetInRange() {
        Dimension dimension = mock(Dimension.class);
        EntityIntelligent spider = spider(dimension, true);
        SpiderAttackExecutor executor = new SpiderAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.2f, 32, false, 12, 1.6);

        executor.onStart(spider);
        // Mesafe 4 blok (kare 16): 2-6 blok araliginda. Sans %65 oldugu icin tekrar denenir.
        EntityLiving target = target(dimension, 4, 0);
        for (int i = 0; i < 50 && executor.leapCooldown == 0; i++) {
            executor.onChase(spider, target, 16);
        }

        ArgumentCaptor<Double> y = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> x = ArgumentCaptor.forClass(Double.class);
        verify(spider, times(1)).setMotion(x.capture(), y.capture(), org.mockito.ArgumentMatchers.anyDouble());
        assertEquals(0.7, x.getValue(), 1e-9);
        assertEquals(0.42, y.getValue(), 1e-9);
        verify(spider).resetFallDistance();
        assertEquals(35, executor.leapCooldown);
    }

    @Test
    void spiderDoesNotLeapWhenTooCloseTooFarOrAirborne() {
        Dimension dimension = mock(Dimension.class);
        EntityIntelligent grounded = spider(dimension, true);
        EntityIntelligent airborne = spider(dimension, false);
        SpiderAttackExecutor executor = new SpiderAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.2f, 32, false, 12, 1.6);
        executor.onStart(grounded);
        EntityLiving near = target(dimension, 1, 0);
        EntityLiving far = target(dimension, 9, 0);
        EntityLiving mid = target(dimension, 4, 0);

        for (int i = 0; i < 100; i++) {
            executor.onChase(grounded, near, 1);
            executor.onChase(grounded, far, 81);
            executor.onChase(airborne, mid, 16);
        }
        verify(grounded, never()).setMotion(anyDouble(), anyDouble(), anyDouble());
        verify(airborne, never()).setMotion(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void witherSkeletonAppliesWitherForTenSeconds() {
        EntityIntelligent skeleton = mock(EntityIntelligent.class);
        EntityLiving target = mock(EntityLiving.class);
        WitherTouchMeleeExecutor executor = new WitherTouchMeleeExecutor(MemoryTypes.ATTACK_TARGET, 0.13f, 32, true, 20, 1.7);

        executor.onAttackSuccess(skeleton, target);

        ArgumentCaptor<EffectInstance> captor = ArgumentCaptor.forClass(EffectInstance.class);
        verify(target).addEffect(captor.capture());
        EffectInstance effect = captor.getValue();
        assertEquals(EffectTypes.WITHER, effect.getType());
        assertEquals(0, effect.getAmplifier());
        assertEquals(200, effect.getDuration());
        assertTrue(effect.isVisible());
    }
}
