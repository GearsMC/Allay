package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Zombi domuz adam için canlı varlık bileşeni.
 *
 * <p>Vanilla değerleri: 20 can, ateşe dayanıklı; ölünce 0-1 çürük et ve 0-1 altın parçası
 * (Yağma seviye başına bir fazla), %2,5 ihtimalle altın külçesi (seviye başına +%1); 5 deneyim.</p>
 */
public class EntityZombiePigmanLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityZombiePigmanLivingComponentImpl() {
        setMaxHealth(20);
    }

    @Override
    public boolean isFireproof() {
        return true;
    }

    @Override
    public boolean hasFireDamage() {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int flesh = rand.nextInt(2 + lootingLevel);
        if (flesh > 0) {
            drops.add(ItemTypes.ROTTEN_FLESH.createItemStack(flesh));
        }
        int nuggets = rand.nextInt(2 + lootingLevel);
        if (nuggets > 0) {
            drops.add(ItemTypes.GOLD_NUGGET.createItemStack(nuggets));
        }
        if (rand.nextFloat() < 0.025f + 0.01f * lootingLevel) {
            drops.add(ItemTypes.GOLD_INGOT.createItemStack());
        }
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
