package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demir golem için canlı varlık bileşeni.
 *
 * <p>Vanilla değerleri: 100 can, düşme hasarı yok, ölünce 3-5 demir külçesi ve 0-2 gelincik;
 * Yağma ganimeti artırmaz, deneyim bırakmaz.</p>
 */
public class EntityIronGolemLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityIronGolemLivingComponentImpl() {
        setMaxHealth(100);
    }

    @Override
    public boolean hasFallDamage() {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        drops.add(ItemTypes.IRON_INGOT.createItemStack(3 + rand.nextInt(3)));
        int poppies = rand.nextInt(3);
        if (poppies > 0) {
            drops.add(ItemTypes.POPPY.createItemStack(poppies));
        }
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 0;
    }
}
