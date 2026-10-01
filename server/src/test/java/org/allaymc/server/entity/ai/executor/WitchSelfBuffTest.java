package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HeartCore {@code Witch::maybeStartSelfBuff} sirasi ve etkileri. */
class WitchSelfBuffTest {

    private static PotionAttackExecutor executor() {
        return new PotionAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.11f, 32, 8, 3, true, 60);
    }

    private static EntityIntelligent witch(float health, boolean onFire) {
        EntityIntelligent witch = mock(EntityIntelligent.class);
        when(witch.getHealth()).thenReturn(health);
        when(witch.getMaxHealth()).thenReturn(26f);
        when(witch.isOnFire()).thenReturn(onFire);
        return witch;
    }

    @Test
    void healthyWitchNearTargetDrinksNothing() {
        assertNull(executor().chooseBrew(witch(26f, false), 25.0));
    }

    @Test
    void burningWitchDrinksFireResistanceFirst() {
        assertEquals(PotionAttackExecutor.Brew.FIRE_RESISTANCE, executor().chooseBrew(witch(5f, true), 400.0));
    }

    @Test
    void woundedWitchDrinksHealing() {
        // 26 * 0.35 = 9.1: 9 canin altinda iyilestirir.
        assertEquals(PotionAttackExecutor.Brew.HEALING, executor().chooseBrew(witch(9f, false), 25.0));
        assertNull(executor().chooseBrew(witch(10f, false), 25.0));
    }

    @Test
    void distantTargetMakesWitchDrinkSpeed() {
        assertEquals(PotionAttackExecutor.Brew.SPEED, executor().chooseBrew(witch(26f, false), 144.0));
        assertNull(executor().chooseBrew(witch(26f, false), 143.0));
    }

    @Test
    void alreadyHasteSkipsSpeedAndBuffCooldownBlocksEverything() {
        EntityIntelligent hasted = witch(26f, false);
        when(hasted.hasEffect(EffectTypes.SPEED)).thenReturn(true);
        assertNull(executor().chooseBrew(hasted, 400.0));

        PotionAttackExecutor cooling = executor();
        cooling.selfBuffCooldown = 10;
        assertNull(cooling.chooseBrew(witch(1f, true), 400.0));
    }

    @Test
    void finishingHealingRestoresFourHealthAndStartsCooldown() {
        PotionAttackExecutor executor = executor();
        EntityIntelligent witch = witch(8f, false);
        executor.brew = PotionAttackExecutor.Brew.HEALING;

        executor.finishDrinking(witch);

        verify(witch).setHealth(12f);
        assertEquals(PotionAttackExecutor.SELF_BUFF_COOLDOWN_TICKS, executor.selfBuffCooldown);
        assertNull(executor.brew);
    }

    @Test
    void finishingSpeedAddsLongSpeedEffect() {
        PotionAttackExecutor executor = executor();
        EntityIntelligent witch = witch(26f, false);
        executor.brew = PotionAttackExecutor.Brew.SPEED;

        executor.finishDrinking(witch);

        ArgumentCaptor<EffectInstance> captor = ArgumentCaptor.forClass(EffectInstance.class);
        verify(witch).addEffect(captor.capture());
        assertEquals(EffectTypes.SPEED, captor.getValue().getType());
        assertEquals(20 * 140, captor.getValue().getDuration());
    }
}
