package org.allaymc.server.world.storage.leveldb;

import lombok.SneakyThrows;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.AllayNBTUtils;
import org.allaymc.api.world.World;
import org.allaymc.api.world.dimension.DimensionType;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.server.world.storage.leveldb.data.LevelDBKey;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.iq80.leveldb.DB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Eski bicimli varlik listesindeki yuklenemeyen kayitlarin dusuruldugunu
 * dogrular.
 *
 * <p>Yeni bicimli dal ({@code readEntitiesSync}) yetim kimlikleri okurken
 * siliyor, eski bicimli dal ise silmiyordu. Chunk kaydedilince yeni bicime
 * gecilir — ama yalnizca yuklenebilen en az bir varlik varsa; hepsi
 * yuklenemeyen bir chunk'ta {@code writeEntities0} kimlik anahtarini bos
 * haritada sildigi icin eski liste yerinde kalir ve <b>ayni hata her acilista
 * yeniden basilir</b>.</p>
 *
 * <p>PocketMine'dan alinan hub dunyalarinda tam olarak bu durum vardi: eklenti
 * varliklarinin kimligi PHP sinif adi ({@code brokiem\snpc\entity\npc\AllayNPC})
 * oldugu icin okunamiyor, her acilista 46 hata satiri basiliyordu.</p>
 */
@ExtendWith(AllayTestExtension.class)
class LegacyEntityListCleanupTest {

    private static final int CHUNK_X = 3;
    private static final int CHUNK_Z = 7;

    @TempDir
    Path tempDir;

    AllayLevelDBWorldStorage storage;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        copyTestResource("ldbworld/level.dat");
        var mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getPlayers()).thenReturn(List.of());
        Mockito.when(mockWorld.getTick()).thenReturn(0L);
        Mockito.when(mockWorld.getDimension(Mockito.any(DimensionType.class)))
                .thenReturn(Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension());
        storage = new AllayLevelDBWorldStorage(tempDir.resolve("ldbworld"));
        storage.setWorld(mockWorld);
    }

    @AfterEach
    @SneakyThrows
    void tearDown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    @Test
    void unloadableEntriesAreDroppedAndLoadableOnesSurvive() {
        writeLegacyList(pocketMineNpc(), painting(), pocketMineNpc());

        var loaded = storage.readEntitiesOldSync(CHUNK_X, CHUNK_Z, DimensionTypes.OVERWORLD);
        assertEquals(1, loaded.size(), "yalnizca tablo yuklenebilmeli");

        var remaining = readLegacyList();
        assertNotNull(remaining, "yuklenebilen varlik kaldigi icin liste durmali");
        assertEquals(1, remaining.size(), "yuklenemeyen kayitlar listeden dusmeli");
        assertEquals("Painting", remaining.getFirst().getString("identifier"));

        // Ikinci okuma artik hicbir hata basmamali.
        assertEquals(1, storage.readEntitiesOldSync(CHUNK_X, CHUNK_Z, DimensionTypes.OVERWORLD).size());
    }

    @Test
    void aListThatIsEntirelyUnloadableIsRemoved() {
        writeLegacyList(pocketMineNpc(), pocketMineNpc());

        assertEquals(0, storage.readEntitiesOldSync(CHUNK_X, CHUNK_Z, DimensionTypes.OVERWORLD).size());

        assertNull(readLegacyList(), "tamamen yuklenemeyen liste silinmeli, yoksa hata her acilista tekrarlanir");
    }

    @Test
    void aFullyLoadableListIsLeftUntouched() {
        writeLegacyList(painting(), painting());

        assertEquals(2, storage.readEntitiesOldSync(CHUNK_X, CHUNK_Z, DimensionTypes.OVERWORLD).size());

        var remaining = readLegacyList();
        assertNotNull(remaining);
        assertEquals(2, remaining.size(), "saglam listeye dokunulmamali");
    }

    private static NbtMap pocketMineNpc() {
        // Hub dunyasindaki gercek kayit: kimlik PHP sinif adi, iki nokta yok.
        return NbtMap.builder()
                .putString("identifier", "brokiem\\snpc\\entity\\npc\\AllayNPC")
                .putList("Pos", NbtType.FLOAT, 268.35f, 99.0f, 265.48f)
                .putList("Rotation", NbtType.FLOAT, 245.06f, 1.71f)
                .putLong("UniqueID", 42L)
                .build();
    }

    private static NbtMap painting() {
        return NbtMap.builder()
                .putString("identifier", "Painting")
                .putList("Pos", NbtType.FLOAT, 268.0f, 99.0f, 284.0f)
                .putList("Rotation", NbtType.FLOAT, 0.0f, 0.0f)
                .putString("Motive", "Kebab")
                .putByte("Direction", (byte) 0)
                .build();
    }

    private byte[] legacyKey() {
        return LevelDBKey.ENTITIES.createKey(CHUNK_X, CHUNK_Z, DimensionTypes.OVERWORLD);
    }

    @SneakyThrows
    private DB db() {
        var field = AllayLevelDBWorldStorage.class.getDeclaredField("db");
        field.setAccessible(true);
        return (DB) field.get(storage);
    }

    private void writeLegacyList(NbtMap... entries) {
        db().put(legacyKey(), AllayNBTUtils.nbtListToBytesLE(List.of(entries)));
    }

    private List<NbtMap> readLegacyList() {
        var bytes = db().get(legacyKey());
        return bytes == null ? null : AllayNBTUtils.bytesToNbtListLE(bytes);
    }

    private void copyTestResource(String resourcePath) throws IOException {
        Path targetPath = tempDir.resolve(resourcePath);
        Files.createDirectories(targetPath.getParent());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                Files.copy(is, targetPath);
            }
        }
    }
}
