package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectType;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.world.data.Difficulty;

import java.util.concurrent.ThreadLocalRandom;

public class MobMeleeAttackExecutor extends MeleeAttackExecutor {

    @FunctionalInterface
    public interface HitEffect {
        void apply(EntityIntelligent attacker, EntityLiving target);
    }

    protected final float minDamage;
    protected final float maxDamage;
    protected final HitEffect hitEffect;

    protected double knockback = -1;
    protected double knockbackVertical = -1;

    public MobMeleeAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                  boolean clearTargetAfterLose, int coolDown, double attackRange,
                                  float damage, HitEffect hitEffect) {
        this(targetIdMemory, speed, maxSenseRange, clearTargetAfterLose, coolDown, attackRange, damage, damage, hitEffect);
    }

    public MobMeleeAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                  boolean clearTargetAfterLose, int coolDown, double attackRange,
                                  float minDamage, float maxDamage, HitEffect hitEffect) {
        super(targetIdMemory, speed, maxSenseRange, clearTargetAfterLose, coolDown, attackRange);
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.hitEffect = hitEffect;
    }

    public MobMeleeAttackExecutor knockback(double horizontal, double vertical) {
        this.knockback = horizontal;
        this.knockbackVertical = vertical;
        return this;
    }

    public static float scaleForDifficulty(float normalDamage, Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0f;
            case EASY -> Math.min(normalDamage / 2f + 1f, normalDamage);
            case NORMAL -> normalDamage;
            case HARD -> normalDamage * 1.5f;
        };
    }

    public static HitEffect effect(EffectType type, int amplifier, int durationTicks) {
        return (attacker, target) -> target.addEffect(new EffectInstance(type, amplifier, durationTicks, false, true));
    }

    @Override
    protected float getAttackDamage(EntityIntelligent entity, EntityLiving victim) {
        var normal = maxDamage > minDamage
                ? (float) ThreadLocalRandom.current().nextDouble(minDamage, maxDamage)
                : minDamage;
        var damage = scaleForDifficulty(normal, entity.getWorld().getWorldData().getDifficulty());
        if (damage > 0 && entity instanceof EntityContainerHolderComponent holder
                && holder.hasContainer(ContainerTypes.ENTITY_HAND)) {
            var weapon = holder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand();
            damage = Math.max(damage, weapon.calculateAttackDamage(entity, victim));
        }
        return damage;
    }

    @Override
    protected DamageContainer createAttackDamage(EntityIntelligent entity, EntityLiving victim, float damage) {
        var container = super.createAttackDamage(entity, victim, damage);
        if (knockback >= 0) {
            container.setKnockback(knockback);
            container.setKnockbackVertical(knockbackVertical);
        }
        return container;
    }

    @Override
    protected void onAttackSuccess(EntityIntelligent entity, EntityLiving target) {
        if (hitEffect != null) {
            hitEffect.apply(entity, target);
        }
    }
}
