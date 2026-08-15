package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creeper icin canli varlik bileseni.
 */
public class EntityCreeperLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityCreeperLivingComponentImpl() {
        setMaxHealth(20);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int gunpowderCount = rand.nextInt(3) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (gunpowderCount > 0) {
            drops.add(ItemTypes.GUNPOWDER.createItemStack(gunpowderCount));
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
