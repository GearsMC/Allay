package org.allaymc.server.world.storage.leveldb.codec;

import io.netty.buffer.Unpooled;
import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.server.world.chunk.AllayChunkBuilder;
import org.allaymc.server.world.chunk.AllayUnsafeChunk;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(AllayTestExtension.class)
class HeightAndBiomeCodecTest {

    @Test
    void testSerializeAndDeserialize() {
        var chunk = AllayUnsafeChunk.builder().voidChunk(0, 0, DimensionTypes.OVERWORLD);

        // Set custom heights and biomes
        chunk.setHeight(0, 0, (short) 100);
        chunk.setHeight(5, 5, (short) 200);
        chunk.setHeight(15, 15, (short) 319);

        chunk.setBiome(0, 0, 0, BiomeTypes.FOREST);
        chunk.setBiome(5, 64, 5, BiomeTypes.DESERT);

        byte[] data = HeightAndBiomeCodec.serialize(chunk);

        // Deserialize into a new builder
        var builder = new AllayChunkBuilder()
                .chunkX(0)
                .chunkZ(0)
                .dimensionType(DimensionTypes.OVERWORLD);

        // Builder needs sections to populate biomes into
        var sections = new org.allaymc.server.world.chunk.AllayChunkSection[DimensionTypes.OVERWORLD.chunkSectionCount()];
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new org.allaymc.server.world.chunk.AllayChunkSection((byte) (i + DimensionTypes.OVERWORLD.minSectionY()));
        }
        builder.sections(sections);

        HeightAndBiomeCodec.deserialize(data, builder);

        // Verify heights
        assertEquals(100, builder.getHeightMap().get(0, 0));
        assertEquals(200, builder.getHeightMap().get(5, 5));
        assertEquals(319, builder.getHeightMap().get(15, 15));

        // Verify biomes - section index for y=0 is (0 - (-64)) / 16 = 4
        var sectionForY0 = sections[0 - DimensionTypes.OVERWORLD.minSectionY()];
        assertEquals(BiomeTypes.FOREST, sectionForY0.getBiomeType(0, 0, 0));

        // section index for y=64 is (64 - (-64)) / 16 = 8
        var sectionForY64 = sections[64 / 16 - DimensionTypes.OVERWORLD.minSectionY()];
        assertEquals(BiomeTypes.DESERT, sectionForY64.getBiomeType(5, 0, 5));
    }

    /**
     * PocketMine biyom paletini kalici (NBT) bayragiyla damgaliyor: baslik baytini
     * {@code (bits << 1) | 0} yaziyor, oysa yuk yine ham LE int. Vanilla ayni durumda
     * {@code | 1} yaziyor. Paletin int okuyucusu bayragi gorup istisna atarsa PM'den
     * gelen her chunk okunamaz olur; bu test o toleransi korur.
     */
    @Test
    void testDeserializePocketMineStyleBiomePalette() {
        var dimensionType = DimensionTypes.OVERWORLD;
        var biomeId = BiomeTypes.FOREST.getId();

        // 512 baytlik yukseklik haritasi + her bolum icin: baslik 0x00 + 4 baytlik biyom kimligi
        var buffer = Unpooled.buffer();
        for (int i = 0; i < HeightAndBiomeCodec.HEIGHTMAP_SIZE; i++) {
            buffer.writeShortLE(0);
        }
        for (int y = dimensionType.minSectionY(); y <= dimensionType.maxSectionY(); y++) {
            buffer.writeByte(0x00);
            buffer.writeIntLE(biomeId);
        }
        var data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);

        var sections = new org.allaymc.server.world.chunk.AllayChunkSection[dimensionType.chunkSectionCount()];
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new org.allaymc.server.world.chunk.AllayChunkSection((byte) (i + dimensionType.minSectionY()));
        }
        var builder = new AllayChunkBuilder()
                .chunkX(0)
                .chunkZ(0)
                .dimensionType(dimensionType)
                .sections(sections);

        assertDoesNotThrow(() -> HeightAndBiomeCodec.deserialize(data, builder));
        for (var section : sections) {
            assertEquals(BiomeTypes.FOREST, section.getBiomeType(0, 0, 0));
        }
    }
}
