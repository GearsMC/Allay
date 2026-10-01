package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tavsan icin canli varlik bileseni. Vanilla ganimeti: tavsan postu, cig tavsan eti, nadiren tavsan ayagi.
 */
public class EntityRabbitLivingComponentImpl extends EntityLivingComponentImpl {

    public EntityRabbitLivingComponentImpl() {
        setMaxHealth(3);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int hides = rand.nextInt(2) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (hides > 0) {
            drops.add(ItemTypes.RABBIT_HIDE.createItemStack(hides));
        }
        int meat = rand.nextInt(2) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (meat > 0) {
            drops.add((isOnFire() ? ItemTypes.COOKED_RABBIT : ItemTypes.RABBIT).createItemStack(meat));
        }
        if (rand.nextInt(10) == 0) {
            drops.add(ItemTypes.RABBIT_FOOT.createItemStack());
        }
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return ThreadLocalRandom.current().nextInt(1, 4);
    }
}
