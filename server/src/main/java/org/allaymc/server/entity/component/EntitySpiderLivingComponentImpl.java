package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orumcek icin canli varlik bileseni. Vanilla ganimeti: ip ve nadiren orumcek gozu.
 */
public class EntitySpiderLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntitySpiderLivingComponentImpl() {
        setMaxHealth(16);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int stringCount = rand.nextInt(3) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (stringCount > 0) {
            drops.add(ItemTypes.STRING.createItemStack(stringCount));
        }
        if (rand.nextInt(3) == 0) {
            int eyes = 1 + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
            drops.add(ItemTypes.SPIDER_EYE.createItemStack(eyes));
        }
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
