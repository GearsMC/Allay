package org.allaymc.server.entity.component;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Living component implementation for piglins.
 */
public class EntityPiglinLivingComponentImpl extends EntityHostileLivingComponentImpl {

    /**
     * Chance to drop the carried weapon, matching vanilla. Kept low on purpose: piglins spawn in
     * waves from GearsCore nether mob blocks, and a guaranteed drop would flood the economy with
     * golden swords and crossbows.
     */
    protected static final float WEAPON_DROP_CHANCE = 0.085f;

    public EntityPiglinLivingComponentImpl() {
        setMaxHealth(16);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        if (containerHolderComponent == null) {
            return drops;
        }

        var rand = ThreadLocalRandom.current();
        if (rand.nextFloat() >= WEAPON_DROP_CHANCE + (0.01f * lootingLevel)) {
            return drops;
        }

        var weapon = containerHolderComponent.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand();
        if (weapon.getItemType() != ItemTypes.AIR) {
            drops.add(weapon.copy());
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
