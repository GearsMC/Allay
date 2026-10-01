package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;

/**
 * Wither iskeletinin yakin dovusu: her isabette hedefe 10 saniye Wither etkisi verir.
 *
 * <p>HeartCore {@code WitherSkeletonAttackStrategy::onSuccessfulAttack}: {@code Wither} etkisi,
 * seviye 1 (amplifier 0), 20 * 10 tick.</p>
 */
public class WitherTouchMeleeExecutor extends MeleeAttackExecutor {

    protected static final int WITHER_DURATION_TICKS = 20 * 10;

    public WitherTouchMeleeExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                    boolean clearTargetAfterLose, int coolDown, double attackRange) {
        super(targetIdMemory, speed, maxSenseRange, clearTargetAfterLose, coolDown, attackRange);
    }

    @Override
    protected void onAttackSuccess(EntityIntelligent entity, EntityLiving target) {
        target.addEffect(new EffectInstance(EffectTypes.WITHER, 0, WITHER_DURATION_TICKS, false, true));
    }
}
