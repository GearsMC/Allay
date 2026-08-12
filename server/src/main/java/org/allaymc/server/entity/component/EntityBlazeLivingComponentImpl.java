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
 * Blaze icin canli varlik bileseni.
 *
 * <p>Iki vanilla ozelligi davranis grubunda degil burada duruyor, cunku blaze bir sey yapiyor
 * olsa da olmasa da gecerliler: yakilamaz, ve su ona zarar verir.</p>
 */
public class EntityBlazeLivingComponentImpl extends EntityHostileLivingComponentImpl {

    /** Vanilla ile ayni: su icinde her seferinde verilen hasar. */
    protected static final float WATER_DAMAGE = 1;

    /** Iki su hasari arasindaki tick sayisi. */
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
        // Blaze ucar; hicbir zaman canini yakacak kadar sert inmez.
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
        // Dokunulmazlik penceresi yok sayiliyor: vanilla'da su hasari duzenli araliklarla isler,
        // her vurustan sonraki yarim saniyelik koruma tarafindan yutulmaz.
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
