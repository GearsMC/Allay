package org.allaymc.server.entity.component;

import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fantom icin canli varlik bileseni. Vanilla ganimeti: fantom zari. Ucan oldugu icin dusme hasari almaz.
 */
public class EntityPhantomLivingComponentImpl extends EntityHostileLivingComponentImpl {

    /** PHP {@code SUNLIGHT_DAMAGE_INTERVAL}: gun isiginda saniyede bir yanma + 1 hasar. */
    protected static final int SUNLIGHT_DAMAGE_INTERVAL = 20;
    /** PHP: zaman {@code [0, 12300)} araligi gundüzdur. */
    protected static final int DAY_END_TICK = 12300;
    protected static final int ON_FIRE_TICKS = 60;

    protected int sunlightCooldown;

    public EntityPhantomLivingComponentImpl() {
        setMaxHealth(20);
    }

    /**
     * Gun isiginda yanma — HeartCore {@code Phantom::handleSunlightDamage}. Elle kontrolde
     * (evcil hayvan) calismaz.
     */
    @EventHandler
    protected void onSunlightTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive() || aiComponent.isManualControlEnabled()) {
            return;
        }
        if (sunlightCooldown > 0) {
            sunlightCooldown--;
            return;
        }
        var dimension = thisEntity.getDimension();
        if (!dimension.getDimensionType().hasSkyLight()) {
            return;
        }
        int time = Math.floorMod(dimension.getWorld().getWorldData().getTimeOfDay(), 24000);
        if (time >= DAY_END_TICK) {
            return;
        }
        var location = thisEntity.getLocation();
        int x = (int) Math.floor(location.x());
        int y = (int) Math.floor(location.y());
        int z = (int) Math.floor(location.z());
        if (!dimension.canPosSeeSky(x, y, z) || thisEntity.isTouchingWater() || hasEffect(EffectTypes.FIRE_RESISTANCE)) {
            return;
        }
        sunlightCooldown = SUNLIGHT_DAMAGE_INTERVAL;
        setOnFireTicks(ON_FIRE_TICKS);
        attack(DamageContainer.fireTick(1f));
    }

    @Override
    public boolean hasFallDamage() {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int membranes = rand.nextInt(2) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (membranes > 0) {
            drops.add(ItemTypes.PHANTOM_MEMBRANE.createItemStack(membranes));
        }
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
