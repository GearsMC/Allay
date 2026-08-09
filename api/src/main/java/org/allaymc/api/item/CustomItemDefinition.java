package org.allaymc.api.item;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.allaymc.api.item.data.ArmorTier;
import org.allaymc.api.item.data.ItemTag;
import org.cloudburstmc.nbt.NbtMap;
import org.allaymc.api.item.data.ToolTier;
import org.allaymc.api.message.MayContainTrKey;

import java.util.Map;
import java.util.Set;

/**
 * Everything needed to register one resource-pack driven custom item.
 * <p>
 * This is the single entry point for custom items, from a plain currency token to a full tool or
 * armour piece. It is a value object rather than a long parameter list so that new traits can be
 * added later without breaking existing callers.
 * <p>
 * The client-side texture must already be declared in a loaded resource pack
 * ({@code item_texture.json}); this definition only describes the server-side type and the
 * component-based definition sent to clients on login.
 *
 * @author GearsMC
 */
@Getter
@Builder
@Accessors(fluent = true)
public class CustomItemDefinition {

    /** The item identifier, e.g. {@code "core:star"}. */
    private final String identifier;

    /**
     * The texture short-name declared in {@code item_texture.json}.
     * <p>
     * Defaults to the identifier, which is the usual convention.
     */
    private final String texture;

    /** The hover/display name; may be a translation key or contain formatting codes. */
    @MayContainTrKey
    private final String displayName;

    /** What the item behaves as. Determines the stack implementation and the tool-kind tags. */
    @Builder.Default
    private final CustomItemBehavior behavior = CustomItemBehavior.SIMPLE;

    /**
     * The tool tier, for tools.
     * <p>
     * Tier decides which blocks the tool may harvest and how fast; it is applied as the matching
     * item tag, exactly like vanilla tools. Ignored when the behaviour is not a tool.
     */
    private final ToolTier toolTier;

    /**
     * The armour tier, for armour.
     * <p>
     * Ignored when the behaviour is not armour.
     */
    private final ArmorTier armorTier;

    /** The maximum stack size (1..64). Tools and armour are forced to 1. */
    @Builder.Default
    private final int maxStackSize = 64;

    /**
     * The durability, i.e. how many uses before the item breaks.
     * <p>
     * Only meaningful for tools and armour. A value of {@code 0} leaves the item indestructible.
     */
    @Builder.Default
    private final int maxDamage = 0;

    /** The melee damage this item deals. */
    @Builder.Default
    private final int attackDamage = 0;

    /** The armour points this item grants when worn. */
    @Builder.Default
    private final int armorValue = 0;

    /** Whether the item always shows the enchantment glint. */
    @Builder.Default
    private final boolean foil = false;
    /**
     * Hunger points restored when eaten. Only used by {@link CustomItemBehavior#EDIBLE}.
     */
    @Builder.Default
    private final int foodPoints = 0;
    /**
     * Saturation restored when eaten. Only used by {@link CustomItemBehavior#EDIBLE}.
     */
    @Builder.Default
    private final float saturationPoints = 0f;
    /**
     * How long the use animation runs, in game ticks.
     */
    @Builder.Default
    private final int eatingTime = 31;
    /**
     * Whether the item is drunk rather than eaten; changes the use animation.
     */
    @Builder.Default
    private final boolean drink = false;
    /**
     * Whether the item can be consumed with a full hunger bar.
     */
    @Builder.Default
    private final boolean canBeAlwaysEaten = false;

    /**
     * The item type this item can be repaired with in an anvil.
     * <p>
     * A supplier because item types are registered in an order the caller does not control.
     * Leaving it unset simply makes the item unrepairable.
     */
    @Builder.Default
    private final java.util.function.Supplier<org.allaymc.api.item.type.ItemType<?>> repairItem = () -> null;

    /**
     * Extra item tags, merged with the ones the behaviour and the tier contribute.
     * <p>
     * Useful for opting an item into vanilla systems that key off tags, for instance
     * {@code minecraft:allow_offhand}.
     */
    @Builder.Default
    private final Set<ItemTag> extraItemTags = Set.of();

    /**
     * Extra client item components sent verbatim in the item definition.
     * <p>
     * The generated definition already covers what every custom item needs — icon, display
     * name, durability, tags, armour and food. Bedrock also has families whose behaviour lives
     * entirely on the client and is driven by components the server only declares, such as a
     * spear's {@code minecraft:kinetic_weapon} and {@code minecraft:piercing_weapon}. Those
     * cannot be derived from a behaviour alone because their tuning is per item, so they are
     * passed through here.
     * <p>
     * Keys are component names ({@code minecraft:swing_duration}); values are their payloads.
     * Anything set here is merged last and therefore wins over the generated components.
     *
     * @return the extra components, empty when the item needs none
     */
    @Builder.Default
    private final Map<String, NbtMap> customComponents = Map.of();

    /**
     * Extra client item properties sent verbatim inside {@code item_properties}.
     * <p>
     * Properties drive how the client treats the item before any behaviour runs. The most
     * consequential is {@code use_duration}: an item with a positive value is "held to use",
     * so holding the button aims it, while an item without one is "held to mine", so holding
     * breaks blocks. A weapon that is meant to be charged therefore needs it declared here or
     * it will dig instead.
     * <p>
     * Values are ints, floats, strings or compounds, matching what the client expects for the
     * property. Anything set here is merged last and wins over the generated properties.
     *
     * @return the extra properties, empty when the item needs none
     */
    @Builder.Default
    private final Map<String, Object> customProperties = Map.of();

    /**
     * The texture short-name, falling back to the identifier when not set.
     *
     * @return the texture to use
     */
    public String resolvedTexture() {
        return texture == null || texture.isBlank() ? identifier : texture;
    }

    /**
     * The stack size to register, forcing tools and armour to a single item.
     *
     * @return the effective maximum stack size
     */
    public int resolvedMaxStackSize() {
        if (behavior != CustomItemBehavior.SIMPLE) {
            return 1;
        }
        return Math.max(1, Math.min(64, maxStackSize));
    }
}
