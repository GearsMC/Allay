package org.allaymc.server.block;

import org.allaymc.api.block.CustomBlockDefinition;
import org.allaymc.api.block.CustomBlockFactory;
import org.allaymc.api.block.data.BlockStateData;
import org.allaymc.api.block.data.BlockTag;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.server.block.component.BlockStateDataComponentImpl;
import org.allaymc.server.block.impl.BlockBehaviorImpl;
import org.allaymc.server.block.type.AllayBlockType;
import org.allaymc.server.block.type.BlockStateDefinition;
import org.allaymc.server.block.type.CustomBlockDefinitionGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of {@link CustomBlockFactory}.
 * <p>
 * Translates the declared definition into the same pieces {@code BlockTypeInitializer} assembles
 * for a vanilla block: the physical data the server reasons about (hardness, resistance, light),
 * the render definition the client draws from, and the block tags the vanilla rules read. Nothing
 * is special-cased for custom blocks — the builder already treats a block as custom by default, so
 * this only fills in what a plugin can reasonably describe.
 *
 * @author GearsMC
 */
public class AllayCustomBlockFactory implements CustomBlockFactory {

    @Override
    public BlockType<?> registerBlock(CustomBlockDefinition definition) {
        var stateData = BlockStateData.builder()
                .hardness(definition.hardness())
                .explosionResistance(definition.resistance())
                .lightEmission(definition.lightEmission())
                .requiresCorrectToolForDrops(definition.requiresCorrectTool())
                .build();

        return AllayBlockType.builder(BlockBehaviorImpl.class)
                .identifier(definition.identifier())
                // Physical properties are read from here by both the server and the client-side
                // definition generator, so they only need declaring once.
                .addComponents(java.util.Map.of(
                        BlockStateDataComponentImpl.IDENTIFIER,
                        BlockStateDataComponentImpl.ofGlobalStatic(stateData)))
                .setBlockTags(collectBlockTags(definition))
                .blockDefinitionGenerator(CustomBlockDefinitionGenerator.of(
                        state -> buildStateDefinition(definition)))
                .build();
    }

    /**
     * Builds the render definition for every state of the block.
     * <p>
     * A definition without its own geometry renders as a full cube, which is what the vast
     * majority of custom blocks want; supplying geometry only matters for shapes the client
     * cannot infer.
     *
     * @param definition the block being registered
     * @return the state definition to send to clients
     */
    private static BlockStateDefinition buildStateDefinition(CustomBlockDefinition definition) {
        var builder = BlockStateDefinition.builder()
                .materials(BlockStateDefinition.Materials.builder()
                        .any(definition.resolvedTexture()));
        if (definition.geometry() != null && !definition.geometry().isBlank()) {
            builder.geometry(BlockStateDefinition.Geometry.of(definition.geometry()));
        }
        return builder.build();
    }

    /**
     * Collects the tags the block must carry.
     *
     * @param definition the block being registered
     * @return the tags to register the type with
     */
    private static Set<BlockTag> collectBlockTags(CustomBlockDefinition definition) {
        return new HashSet<>(definition.extraBlockTags());
    }
}
