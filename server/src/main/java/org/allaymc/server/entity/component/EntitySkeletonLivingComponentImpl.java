package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Living component implementation for skeletons.
 */
public class EntitySkeletonLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntitySkeletonLivingComponentImpl() {
        setMaxHealth(20);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();

        int arrowCount = rand.nextInt(3) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (arrowCount > 0) {
            drops.add(ItemTypes.ARROW.createItemStack(arrowCount));
        }

        int boneCount = rand.nextInt(3) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (boneCount > 0) {
            drops.add(ItemTypes.BONE.createItemStack(boneCount));
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
