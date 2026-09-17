package org.allaymc.server.block.connection;

import org.allaymc.api.block.property.enums.MinecraftCorner;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.world.dimension.DimensionType;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.server.world.chunk.AllayChunkSection;
import org.allaymc.server.world.storage.leveldb.codec.ChunkSectionCodec;
import org.allaymc.server.world.storage.leveldb.data.LevelDBKey;
import org.allaymc.testutils.AllayTestExtension;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ConnectionWorldMigration}'ı geçici bir LevelDB dünyasında sınar: chunk sınırını aşan bağlantı, merdiven köşesi,
 * nether boyutu, deneme kipi ve bölüm dışı anahtarlara dokunulmaması.
 */
@ExtendWith(AllayTestExtension.class)
class ConnectionWorldMigrationTest {

    private static final byte[] FOREIGN_KEY = "scoreboard".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FOREIGN_VALUE = {1, 2, 3};

    @TempDir
    Path world;

    @Test
    void fixesConnectionsAcrossChunksAndDimensions() throws IOException {
        var overworld = new HashMap<Long, AllayChunkSection>();
        // Chunk sınırı: x=15 (chunk 0) ile x=16 (chunk 1) yan yana iki çit.
        set(overworld, 15, 0, 0, BlockTypes.OAK_FENCE.getDefaultState());
        set(overworld, 16, 0, 0, BlockTypes.OAK_FENCE.getDefaultState());
        // Altın tablo STAIRS/w0/bottom/east/w3 → outer_left; dünya y<0 (negatif bölüm) üzerinde.
        set(overworld, 4, -20, 4, stairs(0));
        set(overworld, 5, -20, 4, stairs(3));
        // Taşa bağlanan cam panel.
        set(overworld, 8, 3, 8, BlockTypes.GLASS_PANE.getDefaultState());
        set(overworld, 8, 3, 7, BlockTypes.STONE.getDefaultState());
        var nether = new HashMap<Long, AllayChunkSection>();
        set(nether, 0, 40, 0, BlockTypes.NETHER_BRICK_FENCE.getDefaultState());
        set(nether, 0, 40, 1, BlockTypes.NETHER_BRICK_FENCE.getDefaultState());
        writeWorld(Map.of(DimensionTypes.OVERWORLD, overworld, DimensionTypes.NETHER, nether));

        var dryRun = ConnectionWorldMigration.migrate(world, true);
        assertEquals(7, dryRun.connectionBlocks());
        assertEquals(6, dryRun.changedBlocks());
        withDb(db -> assertFalse(read(db, DimensionTypes.OVERWORLD, 15, 0, 0).getPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_EAST),
                "deneme kipi dünyaya yazmamalı"));

        var report = ConnectionWorldMigration.migrate(world, false);
        assertEquals(6, report.changedBlocks());
        withDb(db -> {
            assertTrue(read(db, DimensionTypes.OVERWORLD, 15, 0, 0).getPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_EAST));
            assertTrue(read(db, DimensionTypes.OVERWORLD, 16, 0, 0).getPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_WEST));
            assertEquals(MinecraftCorner.OUTER_LEFT, read(db, DimensionTypes.OVERWORLD, 4, -20, 4).getPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER));
            assertTrue(read(db, DimensionTypes.OVERWORLD, 8, 3, 8).getPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH));
            assertTrue(read(db, DimensionTypes.NETHER, 0, 40, 0).getPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_SOUTH));
            assertTrue(read(db, DimensionTypes.NETHER, 0, 40, 1).getPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH));
            assertArrayEquals(FOREIGN_VALUE, db.get(FOREIGN_KEY), "bölüm dışı anahtar değişmemeli");
        });

        // İkinci çalıştırma bir şey değiştirmez.
        assertEquals(0, ConnectionWorldMigration.migrate(world, false).changedBlocks());
    }

    private static BlockState stairs(int direction) {
        return BlockTypes.OAK_STAIRS.getDefaultState().setPropertyValue(BlockPropertyTypes.WEIRDO_DIRECTION, direction);
    }

    private static void set(Map<Long, AllayChunkSection> sections, int x, int y, int z, BlockState state) {
        var section = sections.computeIfAbsent(sectionIndex(x >> 4, y >> 4, z >> 4), unused -> new AllayChunkSection((byte) (y >> 4)));
        section.setBlockState(x & 15, y & 15, z & 15, state, 0);
    }

    private static long sectionIndex(int chunkX, int sectionY, int chunkZ) {
        return ((long) chunkX & 0xFFFFF) << 40 | ((long) chunkZ & 0xFFFFF) << 20 | (sectionY & 0xFFFFF);
    }

    private void writeWorld(Map<DimensionType, Map<Long, AllayChunkSection>> dimensions) throws IOException {
        withDb(db -> {
            dimensions.forEach((dimension, sections) -> sections.forEach((index, section) -> {
                var chunkX = (int) (index >> 40) << 12 >> 12;
                var chunkZ = (int) ((index >> 20) & 0xFFFFF) << 12 >> 12;
                db.put(LevelDBKey.CHUNK_SECTION_PREFIX.createKey(chunkX, chunkZ, section.sectionY(), dimension),
                        ChunkSectionCodec.serialize(section, section.sectionY()));
            }));
            db.put(FOREIGN_KEY, FOREIGN_VALUE);
        });
    }

    private static BlockState read(DB db, DimensionType dimension, int x, int y, int z) {
        var data = db.get(LevelDBKey.CHUNK_SECTION_PREFIX.createKey(x >> 4, z >> 4, y >> 4, dimension));
        assertNotNull(data);
        return ChunkSectionCodec.deserialize(data, y >> 4, x >> 4, z >> 4).getBlockState(x & 15, y & 15, z & 15, 0);
    }

    private interface DbAction {
        void run(DB db) throws IOException;
    }

    private void withDb(DbAction action) throws IOException {
        var options = new Options().createIfMissing(true).compressionType(CompressionType.ZLIB_RAW).blockSize(64 * 1024);
        try (var db = new Iq80DBFactory().open(world.resolve("db").toFile(), options)) {
            action.run(db);
        }
    }
}
