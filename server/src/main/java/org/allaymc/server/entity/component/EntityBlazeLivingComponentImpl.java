package org.allaymc.server.entity.component;

import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Living component implementation for blazes.
 *
 * <p>Two vanilla traits live here rather than in the behavior group, because they hold whether or
 * not the blaze is doing anything: it cannot be burned, and water hurts it.</p>
 */
public class EntityBlazeLivingComponentImpl extends EntityHostileLivingComponentImpl {

    /** Damage per water tick, matching vanilla. */
    protected static final float WATER_DAMAGE = 1;

    /** Ticks between two water damage applications. */
    protected static final int WATER_DAMAGE_INTERVAL = 10;

    protected int waterDamageCooldown;

    public EntityBlazeLivingComponentImpl() {
        setMaxHealth(20);
    }

    @Override
    public boolean isFireproof() {
        return true;
    }

    @Override
    public boolean hasFallDamage() {
        // Blazes fly; they never land hard enough to be hurt by it.
        return false;
    }

    @EventHandler
    protected void onWaterTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive() || !thisEntity.isTouchingWater()) {
            waterDamageCooldown = 0;
            return;
        }

        if (waterDamageCooldown > 0) {
            waterDamageCooldown--;
            return;
        }

        waterDamageCooldown = WATER_DAMAGE_INTERVAL;
        // Ignore the invulnerability window: vanilla water damage ticks steadily rather than
        // being swallowed by the half-second grace period after each hit.
        attack(DamageContainer.magicEffect(WATER_DAMAGE), true);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int rodCount = rand.nextInt(2) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (rodCount > 0) {
            drops.add(ItemTypes.BLAZE_ROD.createItemStack(rodCount));
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 10;
    }
}
