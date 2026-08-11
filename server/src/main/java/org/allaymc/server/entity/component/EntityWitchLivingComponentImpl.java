package org.allaymc.server.entity.component;

import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Living component implementation for witches.
 */
public class EntityWitchLivingComponentImpl extends EntityHostileLivingComponentImpl {

    /**
     * Fraction of magic damage a witch actually takes. Vanilla witches shrug off 85% of it, which
     * is why throwing a harming potion back at one barely scratches it.
     */
    protected static final float MAGIC_DAMAGE_MULTIPLIER = 0.15f;

    public EntityWitchLivingComponentImpl() {
        setMaxHealth(26);
    }

    /**
     * Applies the witch's magic resistance last, after armor and effects have had their say. This
     * is the point where the engine has finished computing the damage and has not yet subtracted
     * it from health.
     */
    @Override
    protected void applyVictim(DamageContainer damage) {
        super.applyVictim(damage);

        if (damage.getDamageType() == DamageType.MAGIC) {
            damage.setFinalDamage(damage.getFinalDamage() * MAGIC_DAMAGE_MULTIPLIER);
        }
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        var rand = ThreadLocalRandom.current();
        // Vanilla rolls a handful of stacks and re-picks the item type for each one.
        int rolls = rand.nextInt(4) + (lootingLevel > 0 ? rand.nextInt(lootingLevel + 1) : 0);
        for (int i = 0; i < rolls; i++) {
            var itemType = switch (rand.nextInt(6)) {
                case 0 -> ItemTypes.GLASS_BOTTLE;
                case 1 -> ItemTypes.GLOWSTONE_DUST;
                case 2 -> ItemTypes.GUNPOWDER;
                case 3 -> ItemTypes.REDSTONE;
                case 4 -> ItemTypes.SPIDER_EYE;
                default -> ItemTypes.SUGAR;
            };
            drops.add(itemType.createItemStack(rand.nextInt(1, 3)));
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
