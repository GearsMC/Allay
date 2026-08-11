package org.allaymc.server.entity.component;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Living component shared by the illagers implemented here — pillager and vindicator.
 *
 * <p>Both have the same health and experience, and both drop only the weapon they carry, and only
 * rarely. The drop chance is kept at vanilla's level on purpose: these spawn in waves from GearsCore
 * mob blocks, so a guaranteed drop would flood the server with crossbows and iron axes.</p>
 */
public class EntityIllagerLivingComponentImpl extends EntityHostileLivingComponentImpl {

    protected static final float WEAPON_DROP_CHANCE = 0.085f;

    public EntityIllagerLivingComponentImpl() {
        setMaxHealth(24);
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
