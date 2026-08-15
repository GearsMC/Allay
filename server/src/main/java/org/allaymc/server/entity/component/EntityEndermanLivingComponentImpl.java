package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enderman icin canli varlik bileseni.
 */
public class EntityEndermanLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityEndermanLivingComponentImpl() {
        setMaxHealth(40);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int pearlCount = rand.nextInt(1 + lootingLevel + 1);
        if (pearlCount > 0) {
            drops.add(ItemTypes.ENDER_PEARL.createItemStack(pearlCount));
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
