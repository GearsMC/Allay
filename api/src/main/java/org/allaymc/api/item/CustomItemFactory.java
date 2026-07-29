package org.allaymc.api.item;

import org.allaymc.api.AllayAPI;
import org.allaymc.api.item.type.ItemType;

/**
 * A factory for registering custom items driven by a resource pack.
 *
 * <p>The client-side textures/attachables must already be declared in a loaded
 * resource pack (via {@code item_texture.json}); this factory only registers
 * the server-side item type and generates the component-based item definition
 * that is sent to clients on login. Registration must happen while the item
 * registry is still open — i.e. in a plugin's {@code onLoad}, before players
 * join.</p>
 *
 * @author GearsMC
 */
public interface CustomItemFactory {
    AllayAPI.APIInstanceHolder<CustomItemFactory> FACTORY = AllayAPI.APIInstanceHolder.create();

    /**
     * Returns the bound factory instance.
     *
     * @return the custom item factory
     */
    static CustomItemFactory getFactory() {
        return FACTORY.get();
    }

    /**
     * Registers a custom item.
     *
     * <p>The definition covers every kind of custom item, from a plain currency token to a tool or
     * an armour piece. A plugin declares what the item <i>is</i>
     * ({@link CustomItemDefinition#behavior()}) and the engine picks the matching item stack
     * implementation and contributes the item tags the vanilla rules read — tool kind, tool tier
     * and armour tier are all item tags in Allay, so a tool registered this way is a real tool:
     * it harvests the blocks its tier allows, at the speed the tier gives, and wears down.</p>
     *
     * @param definition the item to register
     * @return the registered item type
     */
    ItemType<?> registerItem(CustomItemDefinition definition);

    /**
     * Registers a simple stackable custom item (no tool/armor/food behaviour).
     *
     * <p>Suitable for material/token items such as currencies, tickets, keys and
     * bundles whose only client-facing traits are an icon, a display name and a
     * stack size.</p>
     *
     * @param identifier   the item identifier, e.g. {@code "core:star"}
     * @param texture      the texture short-name declared in {@code item_texture.json}
     *                     (typically identical to the identifier)
     * @param displayName  the hover/display name (may contain formatting codes)
     * @param maxStackSize the maximum stack size (1..64)
     * @param foil         whether the item always shows the enchantment glint
     * @return the registered item type
     */
    default ItemType<?> registerSimpleItem(String identifier, String texture, String displayName,
                                           int maxStackSize, boolean foil) {
        return registerItem(CustomItemDefinition.builder()
                .identifier(identifier)
                .texture(texture)
                .displayName(displayName)
                .maxStackSize(maxStackSize)
                .foil(foil)
                .build());
    }
}
