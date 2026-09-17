package org.allaymc.server.block.connection;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.server.Allay;
import org.allaymc.server.world.chunk.AllayChunkSection;
import org.allaymc.server.world.storage.leveldb.codec.ChunkSectionCodec;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mevcut dünyalardaki merdiven, çit, cam panel, parmaklık ve tuzak ipinin 26.50 köşe/bağlantı durumlarını tek seferde yazar
 * (yol haritası Adım 6, Parça 4).
 *
 * <p>GearsMC fork. 26.50 öncesi dünyalar yüklenince {@code BlockStateUpdater_1_26_50} yeni durumları varsayılanla doldurur:
 * çitler bağlantısız, merdivenler düz görünür. Oyun içi bileşenler yalnızca bir komşu değişince düzelttiği için dünya bu
 * araçtan bir kez geçirilir. Chunk yüklenirken düzeltmek yerine araç seçildi: bütün dünyayı gördüğü için chunk sınırında
 * komşu tahmini yok.</p>
 *
 * <p>Yalnızca chunk bölümlerine (blok verisi, LevelDB anahtar etiketi 47) dokunur; yükseklik, biyom, blok varlığı ve varlık
 * kayıtları okunmaz ve yazılmaz. Kural, komşunun yalnızca türüne, yönüne ve yarısına baktığı için (komşunun kendi
 * bağlantısına değil) özgün durumlardan tek geçiş yeterli. Dünyada olmayan bölüm havadır. Bölümler yazılırken güncel biçime
 * ve blok durumu sürümüne yükseltilir; sunucu da ilk kayıtta aynısını yapardı. Tanınmayan durumlar
 * ({@link org.allaymc.server.block.type.PreservedBlockState}) olduğu gibi geri yazılır. Araç eklentileri yüklemediği için
 * GearsCore'un özel blokları ({@code core:nether_mob_block} gibi) günlükte "Unrecognized block state" olarak görünür; bu
 * beklenen bir uyarıdır, veri korunur.</p>
 *
 * <p><b>Sunucu kapalıyken ve yedek alındıktan sonra çalıştırılır</b> (LevelDB kilidi açık dünyayı zaten açmaz):</p>
 * <pre>./gradlew :server:migrateConnections -Pworlds=/yol/dunya1,/yol/dunya2 [-PdryRun=true]</pre>
 */
@Slf4j
public final class ConnectionWorldMigration {

    private static final byte SECTION_TAG = 47;

    private ConnectionWorldMigration() {
    }

    public static void main(String[] args) {
        var dryRun = false;
        var worlds = new ArrayList<Path>();
        for (var arg : args) {
            if (arg.equals("--dry-run")) {
                dryRun = true;
            } else if (!arg.isBlank()) {
                worlds.add(Path.of(arg));
            }
        }
        if (worlds.isEmpty()) {
            log.error("Dünya klasörü verilmedi. Kullanım: ConnectionWorldMigration [--dry-run] <dünya klasörü>...");
            System.exit(2);
        }

        try {
            Allay.initI18n();
            Allay.initAllay();
            for (var world : worlds) {
                var report = migrate(world, dryRun);
                log.info("{} {}: {} bölüm, {} bağlantı bloğu, {} blok düzeltildi ({} bölüm){}", dryRun ? "[deneme]" : "[yazıldı]",
                        world, report.sections(), report.connectionBlocks(), report.changedBlocks(), report.changedSections(),
                        report.changedBlocks() > 0 ? " — " + report.changedByType() : "");
            }
        } catch (Throwable t) {
            log.error("Bağlantı taşıması başarısız", t);
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * @param changedByType blok adı → düzeltilen blok sayısı
     */
    public record Report(int sections, int connectionBlocks, int changedBlocks, int changedSections, Map<String, Integer> changedByType) {
    }

    private record SectionKey(int dimension, int chunkX, int chunkZ, int sectionY) {
        static SectionKey parse(byte[] key) {
            // LevelDBKey#createKey biçimi, tamsayılar küçük uçlu.
            // Üst dünya: x(4) z(4) etiket(1) y(1); diğer boyutlar: x(4) z(4) boyut(4) etiket(1) y(1).
            if (key.length == 10 && key[8] == SECTION_TAG) {
                return new SectionKey(0, readInt(key, 0), readInt(key, 4), key[9]);
            }
            if (key.length == 14 && key[12] == SECTION_TAG) {
                var dimension = readInt(key, 8);
                if (dimension == 1 || dimension == 2) {
                    return new SectionKey(dimension, readInt(key, 0), readInt(key, 4), key[13]);
                }
            }
            return null;
        }

        private static int readInt(byte[] bytes, int offset) {
            return (bytes[offset] & 0xff) | (bytes[offset + 1] & 0xff) << 8 | (bytes[offset + 2] & 0xff) << 16 | (bytes[offset + 3] & 0xff) << 24;
        }
    }

    private record Change(int x, int y, int z, BlockState state) {
    }

    public static Report migrate(Path worldDir, boolean dryRun) throws IOException {
        var dbDir = worldDir.resolve("db");
        if (!Files.isDirectory(dbDir)) {
            throw new IOException("LevelDB klasörü yok: " + dbDir);
        }
        var options = new Options().createIfMissing(false).compressionType(CompressionType.ZLIB_RAW).blockSize(64 * 1024);
        try (var db = new Iq80DBFactory().open(dbDir.toFile(), options)) {
            var keys = new HashMap<SectionKey, byte[]>();
            var sections = readSections(db, keys);

            var changes = new HashMap<SectionKey, List<Change>>();
            var changedByType = new TreeMap<String, Integer>();
            var connectionBlocks = 0;
            var changedBlocks = 0;
            for (var entry : sections.entrySet()) {
                var key = entry.getKey();
                var section = entry.getValue();
                if (section.isAirSection()) {
                    continue;
                }
                for (var x = 0; x < 16; x++) {
                    for (var y = 0; y < 16; y++) {
                        for (var z = 0; z < 16; z++) {
                            var state = section.getBlockState(x, y, z, 0);
                            if (!BlockConnectionRules.isConnectionBlock(state)) {
                                continue;
                            }
                            connectionBlocks++;
                            var worldX = (key.chunkX() << 4) + x;
                            var worldY = (key.sectionY() << 4) + y;
                            var worldZ = (key.chunkZ() << 4) + z;
                            var updated = BlockConnectionRules.update(state, face -> blockAt(sections, key.dimension(),
                                    worldX + face.getOffset().x(), worldY, worldZ + face.getOffset().z()));
                            if (!updated.equals(state)) {
                                changes.computeIfAbsent(key, unused -> new ArrayList<>()).add(new Change(x, y, z, updated));
                                changedByType.merge(state.getBlockType().getIdentifier().toString(), 1, Integer::sum);
                                changedBlocks++;
                            }
                        }
                    }
                }
            }

            if (!dryRun && !changes.isEmpty()) {
                try (var batch = db.createWriteBatch()) {
                    for (var entry : changes.entrySet()) {
                        var section = sections.get(entry.getKey());
                        for (var change : entry.getValue()) {
                            section.setBlockState(change.x(), change.y(), change.z(), change.state(), 0);
                        }
                        batch.put(keys.get(entry.getKey()), ChunkSectionCodec.serialize(section, entry.getKey().sectionY()));
                    }
                    db.write(batch);
                }
            }
            return new Report(sections.size(), connectionBlocks, changedBlocks, changes.size(), changedByType);
        }
    }

    private static Map<SectionKey, AllayChunkSection> readSections(DB db, Map<SectionKey, byte[]> keys) throws IOException {
        var sections = new HashMap<SectionKey, AllayChunkSection>();
        try (var iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var key = SectionKey.parse(entry.getKey());
                if (key == null) {
                    continue;
                }
                sections.put(key, ChunkSectionCodec.deserialize(entry.getValue(), key.sectionY(), key.chunkX(), key.chunkZ()));
                keys.put(key, entry.getKey());
            }
        }
        return sections;
    }

    private static BlockState blockAt(Map<SectionKey, AllayChunkSection> sections, int dimension, int x, int y, int z) {
        var section = sections.get(new SectionKey(dimension, x >> 4, z >> 4, y >> 4));
        if (section == null) {
            return BlockTypes.AIR.getDefaultState();
        }
        return section.getBlockState(x & 15, y & 15, z & 15, 0);
    }
}
