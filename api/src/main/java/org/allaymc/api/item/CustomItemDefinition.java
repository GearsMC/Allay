package org.allaymc.api.item;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.allaymc.api.item.data.ArmorTier;
import org.allaymc.api.item.data.ItemTag;
import org.allaymc.api.item.data.ToolTier;
import org.allaymc.api.message.MayContainTrKey;

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
