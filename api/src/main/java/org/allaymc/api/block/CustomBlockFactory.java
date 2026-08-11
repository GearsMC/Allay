package org.allaymc.api.block;

import org.allaymc.api.AllayAPI;
import org.allaymc.api.block.type.BlockType;

/**
 * Registers custom blocks.
 * <p>
 * The engine already knows how to build a block type and how to describe it to the client; what it
 * did not have was a way for a plugin to ask for one, because the builder and the definition
 * generator both live in the server module. This factory is that seam, mirroring
 * {@link org.allaymc.api.item.CustomItemFactory} for items.
 *
 * @author GearsMC
 */
public interface CustomBlockFactory {

    AllayAPI.APIInstanceHolder<CustomBlockFactory> FACTORY = AllayAPI.APIInstanceHolder.create();

    /**
     * @return the factory instance
     */
    static CustomBlockFactory getFactory() {
        return FACTORY.get();
    }

    /**
     * Registers a custom block and the block item that places it.
     * <p>
     * Must be called while the server is starting, before worlds load: block runtime ids are
     * derived from the registered set, and a block added afterwards would be unknown to clients
     * that already received the palette.
     *
     * @param definition the block to register
     * @return the registered block type
     */
    BlockType<?> registerBlock(CustomBlockDefinition definition);

    /**
     * Registers a plain full-cube block with one texture.
     *
     * @param identifier  the block identifier
     * @param texture     the texture short-name
     * @param displayName the name shown to players
     * @param hardness    how long the block takes to break
     * @return the registered block type
     */
    default BlockType<?> registerSimpleBlock(String identifier, String texture,
                                             String displayName, float hardness) {
        return registerBlock(CustomBlockDefinition.builder()
                .identifier(identifier)
                .texture(texture)
                .displayName(displayName)
                .hardness(hardness)
                .build());
    }
}
