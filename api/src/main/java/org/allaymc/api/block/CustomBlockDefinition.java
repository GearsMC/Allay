package org.allaymc.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.allaymc.api.block.data.BlockTag;
import org.allaymc.api.message.MayContainTrKey;

import java.util.Set;

/**
 * A declarative description of a custom block.
 * <p>
 * Registering a block is otherwise an engine-level job: a block type carries behaviour, state
 * properties, a client-side render definition and a matching block item, and wiring those by hand
 * means reaching into server internals. This definition is the plugin-facing seam — describe the
 * block, and {@link CustomBlockFactory} builds the rest the same way a vanilla block is built, so
 * every rule that applies to vanilla blocks applies to a custom one too.
 * <p>
 * Only the identifier is required. A block with nothing else set is a plain full cube that uses
 * one texture on every face and breaks like stone.
 *
 * @author GearsMC
 */
@Getter
@Builder
@Accessors(fluent = true)
public class CustomBlockDefinition {

    /**
     * The block's identifier, for example {@code core:mob_block}.
     * <p>
     * A matching block item is registered automatically under the same identifier, so the block
     * can be held, given and placed without extra work.
     */
    private final String identifier;

    /**
     * The texture short-name used on every face, as named in the resource pack's
     * {@code terrain_texture.json}. Falls back to the identifier when not set.
     */
    private final String texture;

    /**
     * The name shown to players. May be a translation key.
     */
    @MayContainTrKey
    private final String displayName;

    /**
     * How long the block takes to break, in the same scale vanilla uses: stone is {@code 1.5},
     * dirt {@code 0.5}, obsidian {@code 50}.
     */
    @Builder.Default
    private final float hardness = 1.5f;

    /**
     * How well the block resists explosions. Vanilla stone is {@code 6}.
     */
    @Builder.Default
    private final float resistance = 6.0f;

    /**
     * The light the block emits, 0 to 15.
     */
    @Builder.Default
    private final int lightEmission = 0;

    /**
     * Whether the block requires the correct tool to yield anything when broken.
     */
    @Builder.Default
    private final boolean requiresCorrectTool = true;

    /**
     * The geometry to render, for example {@code geometry.my_block}. When unset the block renders
     * as a full cube.
     */
    private final String geometry;

    /**
     * Extra block tags to register the type with. Tags are how the engine answers questions like
     * "is this block a log" — a custom block that should behave as one needs the tag, not a name
     * that merely looks similar.
     */
    @Builder.Default
    private final Set<BlockTag> extraBlockTags = Set.of();

    /**
     * The texture short-name, falling back to the identifier when not set.
     *
     * @return the texture to use
     */
    public String resolvedTexture() {
        return texture == null || texture.isBlank() ? identifier : texture;
    }

    /**
     * The display name, falling back to the identifier when not set.
     *
     * @return the name to show
     */
    public String resolvedDisplayName() {
        return displayName == null || displayName.isBlank() ? identifier : displayName;
    }
}
