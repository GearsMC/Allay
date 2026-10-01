package org.allaymc.server.entity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wither iskeleti icin canli varlik bileseni: ateste yanmaz; kömür, kemik ve nadiren kafatasi birakir.
 */
public class EntityWitherSkeletonLivingComponentImpl extends EntityHostileLivingComponentImpl {

    public EntityWitherSkeletonLivingComponentImpl() {
        setMaxHealth(20);
    }

    @Override
    public boolean hasFireDamage() {
        return false;
    }

    @Override
    public boolean isFireproof() {
        return true;
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        int coal = rand.nextInt(2) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (coal > 0) {
            drops.add(ItemTypes.COAL.createItemStack(coal));
        }
        int bones = rand.nextInt(3) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        if (bones > 0) {
            drops.add(ItemTypes.BONE.createItemStack(bones));
        }
        if (rand.nextFloat() < 0.025f + 0.01f * lootingLevel) {
            drops.add(ItemTypes.WITHER_SKELETON_SKULL.createItemStack());
        }
        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
