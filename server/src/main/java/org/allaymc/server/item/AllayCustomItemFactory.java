package org.allaymc.server.item;

import org.allaymc.api.item.CustomItemBehavior;
import org.allaymc.api.item.CustomItemDefinition;
import org.allaymc.api.item.CustomItemFactory;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.data.ItemData;
import org.allaymc.api.item.data.ItemTag;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.server.item.impl.ItemAxeStackImpl;
import org.allaymc.server.item.impl.ItemBootsStackImpl;
import org.allaymc.server.item.impl.ItemChestplateStackImpl;
import org.allaymc.server.item.impl.ItemHelmetStackImpl;
import org.allaymc.server.item.impl.ItemHoeStackImpl;
import org.allaymc.server.item.impl.ItemLeggingsStackImpl;
import org.allaymc.server.item.impl.ItemPickaxeStackImpl;
import org.allaymc.server.item.impl.ItemShovelStackImpl;
import org.allaymc.server.item.impl.ItemSpearStackImpl;
import org.allaymc.server.item.impl.ItemStackImpl;
import org.allaymc.server.item.impl.ItemSwordStackImpl;
import org.allaymc.server.item.component.ItemArmorBaseComponentImpl;
import org.allaymc.server.item.component.edible.ItemEdibleComponentImpl;
import org.allaymc.server.item.component.ItemRepairableComponentImpl;
import org.allaymc.server.item.component.tool.ItemHoeToolComponentImpl;
import org.allaymc.server.item.component.tool.ItemSwordToolComponentImpl;
import org.allaymc.server.item.component.tool.ItemToolComponentImpl;
import org.allaymc.server.item.component.ItemTrimmableComponentImpl;
import org.allaymc.server.item.component.ItemWearableComponentImpl;
import org.allaymc.server.item.type.AllayItemType;
import org.allaymc.server.item.type.CustomItemDefinitionGenerator;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link CustomItemFactory}.
 * <p>
 * Translates the declared {@link CustomItemBehavior} into the item stack implementation carrying
 * the matching components, and into the item tags the vanilla rules read. Nothing is special-cased
 * for custom items: a custom pickaxe is registered the same way {@code ItemTypeDefaultInitializer}
 * registers a vanilla one, so every rule that applies to vanilla tools applies to it too.
 *
 * @author GearsMC
 */
public class AllayCustomItemFactory implements CustomItemFactory {

    /** The item stack implementation each behaviour maps to. */
    private static final Map<CustomItemBehavior, Class<? extends ItemStack>> BEHAVIOR_CLASSES =
            new EnumMap<>(CustomItemBehavior.class);

    static {
        BEHAVIOR_CLASSES.put(CustomItemBehavior.SIMPLE, ItemStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.PICKAXE, ItemPickaxeStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.AXE, ItemAxeStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.SHOVEL, ItemShovelStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.HOE, ItemHoeStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.SWORD, ItemSwordStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.SPEAR, ItemSpearStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.HELMET, ItemHelmetStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.CHESTPLATE, ItemChestplateStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.LEGGINGS, ItemLeggingsStackImpl.class);
        BEHAVIOR_CLASSES.put(CustomItemBehavior.BOOTS, ItemBootsStackImpl.class);
        // Food has no dedicated stack implementation in Allay: the edible component
        // carries all the behaviour, so the plain stack is enough.
        BEHAVIOR_CLASSES.put(CustomItemBehavior.EDIBLE, ItemStackImpl.class);
    }

    @Override
    public ItemType<?> registerItem(CustomItemDefinition definition) {
        var behavior = definition.behavior();
        var itemData = ItemData.builder()
                .maxStackSize(definition.resolvedMaxStackSize())
                // A declared max damage is what makes an item damageable, whatever its
                // behaviour: a plain item can be a limited-use charm just as a tool can
                // wear out. Simple items default to 0, so nothing changes for them
                // unless the definition asks for durability explicitly.
                .isDamageable(definition.maxDamage() > 0)
                .maxDamage(definition.maxDamage())
                .attackDamage(definition.attackDamage())
                .armorValue(definition.armorValue())
                .build();

        var builder = AllayItemType.builder(BEHAVIOR_CLASSES.get(behavior))
                .identifier(new Identifier(definition.identifier()))
                .itemData(itemData)
                .setItemTags(collectItemTags(definition))
                .itemDefinitionGenerator(CustomItemDefinitionGenerator.builder()
                        .texture(definition.resolvedTexture())
                        .displayName(definition.displayName())
                        .foil(definition.foil())
                        .customComponents(definition.customComponents())
                        .customProperties(definition.customProperties())
                        .build());
        addBehaviorComponents(builder, definition);
        return builder.build();
    }

    /**
     * Adds the components the chosen stack implementation delegates to.
     * <p>
     * The component set mirrors {@code ItemTypeInitializer} exactly: a custom pickaxe gets the same
     * components a vanilla pickaxe gets, so it behaves identically. Missing one of them would fail
     * at registration time rather than silently, because the stack implementation injects its
     * delegates by component type.
     *
     * @param builder    the type builder being configured
     * @param definition the item being registered
     */
    private static void addBehaviorComponents(AllayItemType.Builder builder, CustomItemDefinition definition) {
        var behavior = definition.behavior();
        if (behavior == CustomItemBehavior.SIMPLE) {
            return;
        }

        if (behavior == CustomItemBehavior.EDIBLE) {
            builder.addComponent(() -> new ItemEdibleComponentImpl(
                            definition.foodPoints(), definition.saturationPoints(),
                            definition.eatingTime(), definition.drink(),
                            definition.canBeAlwaysEaten()),
                    ItemEdibleComponentImpl.class);
            return;
        }

        if (behavior == CustomItemBehavior.SWORD) {
            builder.addComponent(ItemSwordToolComponentImpl::new, ItemSwordToolComponentImpl.class);
        } else if (behavior == CustomItemBehavior.SPEAR) {
            // Vanilla spears carry no tool component: ItemTypeInitializer.buildSpear
            // only adds the repairable component, which is appended below.
            builder.addComponent(ItemToolComponentImpl::new, ItemToolComponentImpl.class);
        } else if (behavior == CustomItemBehavior.HOE) {
            builder.addComponent(ItemHoeToolComponentImpl::new, ItemHoeToolComponentImpl.class);
        } else if (behavior.armorType() == null) {
            builder.addComponent(ItemToolComponentImpl::new, ItemToolComponentImpl.class);
        } else {
            builder.addComponent(ItemArmorBaseComponentImpl::new, ItemArmorBaseComponentImpl.class);
            builder.addComponent(() -> new ItemWearableComponentImpl(behavior.armorType()),
                    ItemWearableComponentImpl.class);
            builder.addComponent(ItemTrimmableComponentImpl::new, ItemTrimmableComponentImpl.class);
        }

        // Tools and armour delegate a repairable component whether or not a repair material is
        // configured; without a material the item simply cannot be repaired in an anvil.
        var repairItem = definition.repairItem();
        builder.addComponent(() -> new ItemRepairableComponentImpl(repairItem),
                ItemRepairableComponentImpl.class);
    }

    /**
     * Collects the tags the item must carry: its tool kind, its tier and anything the caller added.
     * <p>
     * Tier is a tag rather than a field in Allay — {@code ItemHelper.getToolTier} finds a tool's
     * tier by looking for the tier tag — so a tool registered without one can never be the correct
     * tool for a block that requires a tier.
     *
     * @param definition the item being registered
     * @return the tags to register the type with
     */
    private static Set<ItemTag> collectItemTags(CustomItemDefinition definition) {
        var tags = new HashSet<>(definition.behavior().itemTags());
        tags.addAll(definition.extraItemTags());
        if (definition.toolTier() != null) {
            tags.add(definition.toolTier().getItemTag());
        }
        if (definition.armorTier() != null) {
            tags.add(definition.armorTier().getItemTag());
        }
        return tags;
    }
}
