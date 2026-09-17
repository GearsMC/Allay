package org.allaymc.server.world.storage.leveldb.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.server.datastruct.palette.Palette;
import org.allaymc.server.network.ProtocolInfo;
import org.allaymc.server.world.chunk.AllayChunkSection;
import org.allaymc.server.world.storage.leveldb.LevelDBUtils;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.allaymc.api.block.type.BlockTypes.AIR;
import static org.allaymc.api.block.type.BlockTypes.GLASS_PANE;
import static org.allaymc.api.block.type.BlockTypes.IRON_BARS;
import static org.allaymc.api.block.type.BlockTypes.OAK_FENCE;
import static org.allaymc.api.block.type.BlockTypes.OAK_STAIRS;
import static org.allaymc.api.block.type.BlockTypes.TRIP_WIRE;
import static org.allaymc.api.block.type.BlockTypes.OAK_WOOD;
import static org.allaymc.api.block.type.BlockTypes.POTENT_SULFUR;
import static org.allaymc.api.block.type.BlockTypes.STONE;
import static org.allaymc.api.block.type.BlockTypes.UNKNOWN;
import static org.allaymc.api.utils.hash.HashUtils.hashChunkSectionXYZ;
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

    /**
     * Allay'in tanımadığı blok durumu okunurken bilinmeyen bloğa dönüşüyordu. Bölümde başka bir blok değişip bölüm
     * yeniden yazılınca özgün veri kalıcı olarak siliniyordu. Tanınmayan durum daha yeni sürümlü veride (PocketMine
     * 1.26.50 yazıyor), Allay eski sürüme geri alınınca ya da artık kayıtlı olmayan bir eklenti bloğunda çıkar.
     * Özgün NBT korunmalı ve kayıtta aynen geri yazılmalı; sürümü daha yeni ama tanıdık durum normal okunmalı.
     */
    @Test
    void testUnrecognizedBlockStatesSurviveResave() {
        var newerVersion = ProtocolInfo.BLOCK_STATE_VERSION_NUM + 1;
        var futureBlock = NbtMap.builder()
                .putString("name", "minecraft:gears_test_future_block")
                .putCompound("states", NbtMap.builder().putString("gears_test_state", "a").build())
                .putInt("version", newerVersion)
                .build();
        var removedPluginBlock = NbtMap.builder()
                .putString("name", "gears_test:removed_block")
                .putCompound("states", NbtMap.EMPTY)
                .putInt("version", ProtocolInfo.BLOCK_STATE_VERSION_NUM)
                .build();
        var newerStone = STONE.getDefaultState().getBlockStateNBT().toBuilder().putInt("version", newerVersion).build();

        var layer0 = new Palette<>(AIR.getDefaultState().getBlockStateNBT());
        layer0.set(hashChunkSectionXYZ(1, 2, 3), futureBlock);
        layer0.set(hashChunkSectionXYZ(4, 5, 6), removedPluginBlock);
        layer0.set(hashChunkSectionXYZ(7, 8, 9), newerStone);
        var layer1 = new Palette<>(AIR.getDefaultState().getBlockStateNBT());
        var data = LevelDBUtils.withByteBufToArray(buffer -> {
            buffer.writeByte(AllayChunkSection.CURRENT_CHUNK_SECTION_VERSION);
            buffer.writeByte(AllayChunkSection.LAYER_COUNT);
            buffer.writeByte(0);
            layer0.writeToStorage(buffer, tag -> tag);
            layer1.writeToStorage(buffer, tag -> tag);
        });

        var section = ChunkSectionCodec.deserialize(data, 0, 0, 0);
        assertNotNull(section);
        assertEquals(STONE.getDefaultState(), section.getBlockState(7, 8, 9, 0));
        var loadedFuture = section.getBlockState(1, 2, 3, 0);
        assertEquals(UNKNOWN, loadedFuture.getBlockType());
        // İstemciye bilinmeyen blok olarak gider
        assertEquals(UNKNOWN.getDefaultState().blockStateHash(), loadedFuture.blockStateHash());
        assertEquals(UNKNOWN, section.getBlockState(4, 5, 6, 0).getBlockType());

        // Aynı bölümde başka bir blok değişir, bölüm yeniden yazılır
        section.setBlockState(10, 11, 12, OAK_WOOD.getDefaultState(), 0);
        var resaved = readRawLayer0(ChunkSectionCodec.serialize(section, 0));

        assertEquals(futureBlock, resaved.get(hashChunkSectionXYZ(1, 2, 3)));
        assertEquals(removedPluginBlock, resaved.get(hashChunkSectionXYZ(4, 5, 6)));
        assertEquals(STONE.getDefaultState().getBlockStateNBT(), resaved.get(hashChunkSectionXYZ(7, 8, 9)));
        assertEquals(OAK_WOOD.getDefaultState().getBlockStateNBT(), resaved.get(hashChunkSectionXYZ(10, 11, 12)));
    }

    /**
     * Özelliği eklenmeden önce kaydedilmiş potent sulfur (1.26.30 önizlemesi) güncelleyiciden geçince
     * {@code potent_sulfur_state=dry} almalı. Allay 1.21.110 güncelleyicisinde kaldığı sürece bu blok tanınmıyordu.
     */
    @Test
    void testOlderPotentSulfurIsUpgradedBy_1_26_30_Updater() {
        var olderPotentSulfur = NbtMap.builder()
                .putString("name", "minecraft:potent_sulfur")
                .putCompound("states", NbtMap.EMPTY)
                .putInt("version", (1 << 24) | (21 << 16) | (60 << 8) | 33)
                .build();
        var layer0 = new Palette<>(AIR.getDefaultState().getBlockStateNBT());
        layer0.set(hashChunkSectionXYZ(1, 1, 1), olderPotentSulfur);
        var layer1 = new Palette<>(AIR.getDefaultState().getBlockStateNBT());
        var data = LevelDBUtils.withByteBufToArray(buffer -> {
            buffer.writeByte(AllayChunkSection.CURRENT_CHUNK_SECTION_VERSION);
            buffer.writeByte(AllayChunkSection.LAYER_COUNT);
            buffer.writeByte(0);
            layer0.writeToStorage(buffer, tag -> tag);
            layer1.writeToStorage(buffer, tag -> tag);
        });

        var section = ChunkSectionCodec.deserialize(data, 0, 0, 0);

        assertNotNull(section);
        assertEquals(POTENT_SULFUR.getDefaultState(), section.getBlockState(1, 1, 1, 0));
    }

    /**
     * 26.50 öncesi Allay'in yazdığı dünya (blok sürümü 1.21.110.26): merdivende {@code minecraft:corner}, çit, cam panel,
     * parmaklık ve tuzak ipinde {@code minecraft:connection_*} yok. Yüklenince aynı bloğun varsayılan köşe/bağlantı
     * çeşidine yükseltilmeli; tanınmayan blok olarak kalmamalı. Gerçek köşe/bağlantıyı taşıma aracı hesaplar.
     */
    @Test
    void testPre2650SectionIsUpgradedToNewStates() {
        var allay2630 = (1 << 24) | (21 << 16) | (110 << 8) | 26;
        var oldStairs = NbtMap.builder()
                .putString("name", "minecraft:oak_stairs")
                .putCompound("states", NbtMap.builder().putByte("upside_down_bit", (byte) 1).putInt("weirdo_direction", 2).build())
                .putInt("version", allay2630)
                .build();
        var layer0 = new Palette<>(AIR.getDefaultState().getBlockStateNBT());
        layer0.set(hashChunkSectionXYZ(1, 1, 1), oldStairs);
        var names = new String[]{"minecraft:oak_fence", "minecraft:glass_pane", "minecraft:iron_bars"};
        for (int i = 0; i < names.length; i++) {
            layer0.set(hashChunkSectionXYZ(2 + i, 1, 1), NbtMap.builder()
                    .putString("name", names[i])
                    .putCompound("states", NbtMap.EMPTY)
                    .putInt("version", allay2630)
                    .build());
        }
        layer0.set(hashChunkSectionXYZ(6, 1, 1), NbtMap.builder()
                .putString("name", "minecraft:trip_wire")
                .putCompound("states", NbtMap.builder()
                        .putByte("attached_bit", (byte) 0).putByte("disarmed_bit", (byte) 0)
                        .putByte("powered_bit", (byte) 0).putByte("suspended_bit", (byte) 1).build())
                .putInt("version", allay2630)
                .build());
        var layer1 = new Palette<>(AIR.getDefaultState().getBlockStateNBT());
        var data = LevelDBUtils.withByteBufToArray(buffer -> {
            buffer.writeByte(AllayChunkSection.CURRENT_CHUNK_SECTION_VERSION);
            buffer.writeByte(AllayChunkSection.LAYER_COUNT);
            buffer.writeByte(0);
            layer0.writeToStorage(buffer, tag -> tag);
            layer1.writeToStorage(buffer, tag -> tag);
        });

        var section = ChunkSectionCodec.deserialize(data, 0, 0, 0);

        assertNotNull(section);
        assertEquals(OAK_STAIRS.getDefaultState()
                        .setPropertyValue(BlockPropertyTypes.UPSIDE_DOWN_BIT, true)
                        .setPropertyValue(BlockPropertyTypes.WEIRDO_DIRECTION, 2),
                section.getBlockState(1, 1, 1, 0));
        assertEquals(OAK_FENCE.getDefaultState(), section.getBlockState(2, 1, 1, 0));
        assertEquals(GLASS_PANE.getDefaultState(), section.getBlockState(3, 1, 1, 0));
        assertEquals(IRON_BARS.getDefaultState(), section.getBlockState(4, 1, 1, 0));
        assertEquals(TRIP_WIRE.getDefaultState().setPropertyValue(BlockPropertyTypes.SUSPENDED_BIT, true),
                section.getBlockState(6, 1, 1, 0));
    }

    private static Palette<NbtMap> readRawLayer0(byte[] data) {
        var buffer = Unpooled.wrappedBuffer(data);
        buffer.skipBytes(3); // bölüm sürümü, katman sayısı, bölüm y
        var palette = new Palette<>(NbtMap.EMPTY);
        palette.readFromStorage(buffer, ChunkSectionCodecTest::readTag);
        return palette;
    }

    private static NbtMap readTag(ByteBuf buffer) {
        try (var input = NbtUtils.createReaderLE(new ByteBufInputStream(buffer))) {
            return (NbtMap) input.readTag();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
