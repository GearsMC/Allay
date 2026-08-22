package org.allaymc.server.world.storage.leveldb.codec;

import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.server.world.chunk.AllayChunkSection;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.allaymc.api.block.type.BlockTypes.OAK_WOOD;
import static org.allaymc.api.block.type.BlockTypes.STONE;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AllayTestExtension.class)
class ChunkSectionCodecTest {

    @Test
    void testSerializeAndDeserialize() {
        var section = new AllayChunkSection((byte) 0);

        // Set some blocks on layer 0
        section.setBlockState(0, 0, 0, OAK_WOOD.getDefaultState(), 0);
        section.setBlockState(1, 1, 1, STONE.getDefaultState(), 0);

        // Set a block on layer 1
        section.setBlockState(2, 2, 2, OAK_WOOD.getDefaultState(), 1);

        byte[] data = ChunkSectionCodec.serialize(section, 0);
        assertNotNull(data);

        AllayChunkSection deserialized = ChunkSectionCodec.deserialize(data, 0, 0, 0);
        assertNotNull(deserialized);

        assertEquals(OAK_WOOD.getDefaultState(), deserialized.getBlockState(0, 0, 0, 0));
        assertEquals(STONE.getDefaultState(), deserialized.getBlockState(1, 1, 1, 0));
        assertEquals(OAK_WOOD.getDefaultState(), deserialized.getBlockState(2, 2, 2, 1));
    }

    @Test
    void testDeserializeUnknownVersionReturnsNull() {
        // Create a byte array with an unknown version byte (e.g., 99)
        byte[] data = new byte[]{99, 0, 0, 0};
        AllayChunkSection result = ChunkSectionCodec.deserialize(data, 0, 0, 0);
        assertNull(result);
    }

    @Test
    void testFillNullSections() {
        var dimensionType = DimensionTypes.OVERWORLD;
        var sections = new AllayChunkSection[dimensionType.chunkSectionCount()];
        // Leave all sections null
        assertNull(sections[0]);

        ChunkSectionCodec.fillNullSections(sections, dimensionType);

        for (int i = 0; i < sections.length; i++) {
            assertNotNull(sections[i], "Section at index " + i + " should not be null after fill");
            assertEquals((byte) (i + dimensionType.minSectionY()), sections[i].sectionY());
            assertEquals(BiomeTypes.PLAINS, sections[i].getBiomeType(0, 0, 0));
        }
    }

    @Test
    void testFillNullSectionsUsesDimensionDefaultBiome() {
        var netherSections = new AllayChunkSection[DimensionTypes.NETHER.chunkSectionCount()];
        ChunkSectionCodec.fillNullSections(netherSections, DimensionTypes.NETHER);
        assertEquals(BiomeTypes.HELL, netherSections[0].getBiomeType(0, 0, 0));

        var endSections = new AllayChunkSection[DimensionTypes.THE_END.chunkSectionCount()];
        ChunkSectionCodec.fillNullSections(endSections, DimensionTypes.THE_END);
        assertEquals(BiomeTypes.THE_END, endSections[0].getBiomeType(0, 0, 0));
    }
}
