package org.allaymc.api.item;

import org.allaymc.api.item.data.ArmorType;
import org.allaymc.api.item.data.ItemTag;
import org.allaymc.api.item.data.ItemTags;

import java.util.Set;

/**
 * The behaviour a custom item takes on, declared by intent rather than by implementation class.
 * <p>
 * A plugin says <i>"this is a pickaxe"</i> and the engine picks the matching item stack
 * implementation and the item tags the vanilla logic keys off. Tool tier and tool kind are not
 * separate concepts in Allay — they are item tags, and every rule that depends on them
 * ({@link ItemHelper#getToolTier}, {@link ItemHelper#isPickaxe}, block break time, correct-tool
 * checks) reads those tags. Declaring the behaviour therefore has to contribute the kind tag; the
 * tier tag comes from {@link CustomItemDefinition#toolTier()}.
 *
 * @author GearsMC
 */
public enum CustomItemBehavior {

    /** No behaviour; a plain stackable item such as a currency, key or ticket. */
    SIMPLE(Set.of(), null),
    /** Mines stone-family blocks. */
    PICKAXE(Set.of(ItemTags.IS_PICKAXE), null),
    /** Chops wood-family blocks. */
    AXE(Set.of(ItemTags.IS_AXE), null),
    /** Digs dirt-family blocks. */
    SHOVEL(Set.of(ItemTags.IS_SHOVEL), null),
    /** Tills and harvests crops. */
    HOE(Set.of(ItemTags.IS_HOE), null),
    /** A melee weapon. */
    SWORD(Set.of(ItemTags.IS_SWORD), null),
    /** Worn on the head. */
    HELMET(Set.of(), ArmorType.HELMET),
    /** Worn on the chest. */
    CHESTPLATE(Set.of(), ArmorType.CHESTPLATE),
    /** Worn on the legs. */
    LEGGINGS(Set.of(), ArmorType.LEGGINGS),
    /** Worn on the feet. */
    BOOTS(Set.of(), ArmorType.BOOTS);

    private final Set<ItemTag> itemTags;
    private final ArmorType armorType;

    CustomItemBehavior(Set<ItemTag> itemTags, ArmorType armorType) {
        this.itemTags = itemTags;
        this.armorType = armorType;
    }

    /**
     * The armour slot this behaviour is worn in.
     *
     * @return the slot, or {@code null} when the behaviour is not armour
     */
    public ArmorType armorType() {
        return armorType;
    }

    /**
     * The item tags this behaviour contributes, on top of the tier tag.
     *
     * @return the tags identifying the tool kind; empty for non-tools
     */
    public Set<ItemTag> itemTags() {
        return itemTags;
    }

    /**
     * Whether the behaviour wears down and can be repaired.
     *
     * @return {@code true} for tools and armour
     */
    public boolean isDamageable() {
        return this != SIMPLE;
    }
}
