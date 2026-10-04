package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.server.entity.component.EntityHostileLivingComponentImpl;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class EntityMobLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public enum Breathing {
        AIR,
        WATER,
        AMPHIBIOUS
    }

    protected final MobLoot loot;
    protected final ToIntFunction<LootContext> xp;
    protected final Set<DamageType> immunities = new HashSet<>();

    protected Breathing breathing = Breathing.AIR;
    protected boolean fireproof;
    protected int minRandomHealth = -1;
    protected Predicate<DamageContainer> damageFilter = damage -> true;

    public EntityMobLivingComponentImpl(float maxHealth, MobLoot loot, ToIntFunction<LootContext> xp) {
        this.loot = loot;
        this.xp = xp;
        setMaxHealth(maxHealth);
        initHealthToMax();
    }

    public EntityMobLivingComponentImpl randomHealth(int min) {
        this.minRandomHealth = min;
        return this;
    }

    public EntityMobLivingComponentImpl fireproof() {
        this.fireproof = true;
        return this;
    }

    public EntityMobLivingComponentImpl breathing(Breathing breathing) {
        this.breathing = breathing;
        return this;
    }

    public EntityMobLivingComponentImpl immuneTo(DamageType... types) {
        this.immunities.addAll(List.of(types));
        return this;
    }

    public EntityMobLivingComponentImpl damageFilter(Predicate<DamageContainer> filter) {
        this.damageFilter = filter;
        return this;
    }

    @Override
    public boolean canBeAttacked(DamageContainer damage) {
        return !immunities.contains(damage.getDamageType()) && damageFilter.test(damage) && super.canBeAttacked(damage);
    }

    @Override
    public boolean isFireproof() {
        return fireproof;
    }

    @Override
    public boolean hasFallDamage() {
        return !immunities.contains(DamageType.FALL) && super.hasFallDamage();
    }

    @Override
    public boolean hasDrowningDamage() {
        return breathing != Breathing.AMPHIBIOUS && super.hasDrowningDamage();
    }

    @Override
    public boolean canBreathe() {
        return switch (breathing) {
            case AIR -> super.canBreathe();
            case WATER -> thisEntity.isTouchingWater();
            case AMPHIBIOUS -> true;
        };
    }

    @EventHandler
    protected void onRollHealth(CEntityLoadNBTEvent event) {
        if (minRandomHealth < 0 || event.getNbt().containsKey(TAG_MAX_HEALTH)) {
            return;
        }

        var max = (int) getMaxHealth();
        setMaxHealth(ThreadLocalRandom.current().nextInt(Math.min(minRandomHealth, max), max + 1));
        initHealthToMax();
    }

    public LootContext lootContext(int lootingLevel) {
        var attacker = lastDamage == null ? null : resolveAttacker(lastDamage.getAttacker());
        var baby = thisEntity instanceof EntityBabyComponent babyComponent && babyComponent.isBaby();
        return new LootContext(thisEntity, lootingLevel, attacker instanceof EntityPlayer, isOnFire(), baby);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        loot.roll(lootContext(lootingLevel), drops);
        drops.addAll(super.getDrops(lootingLevel));
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return Math.max(0, xp.applyAsInt(lootContext(0))) + super.getDropXpAmount();
    }
}
