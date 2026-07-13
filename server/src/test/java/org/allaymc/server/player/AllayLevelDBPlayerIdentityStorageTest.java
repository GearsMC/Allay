package org.allaymc.server.player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Clexa
 */
class AllayLevelDBPlayerIdentityStorageTest {

    @TempDir
    Path tempDir;

    AllayLevelDBPlayerIdentityStorage storage;

    @BeforeEach
    void setUp() {
        storage = new AllayLevelDBPlayerIdentityStorage(tempDir.resolve("players-meta"));
    }

    @AfterEach
    void tearDown() {
        storage.shutdown();
    }

    @Test
    void testRememberAndLookup() {
        storage.rememberIdentity("2535466392945800", "Lexa");

        assertEquals(Optional.of("2535466392945800"), storage.lookupXuidByName("Lexa"));
        assertEquals(Optional.of("2535466392945800"), storage.lookupXuidByName("lexa"));
        assertEquals(Optional.of("2535466392945800"), storage.lookupXuidByName("LEXA"));
        assertEquals(Optional.of("Lexa"), storage.lookupNameByXuid("2535466392945800"));
        assertEquals(Optional.empty(), storage.lookupXuidByName("Unknown"));
        assertEquals(Optional.empty(), storage.lookupNameByXuid("999"));
    }

    @Test
    void testRenameReplacesOldNameMapping() {
        storage.rememberIdentity("2535466392945800", "OldName");
        storage.rememberIdentity("2535466392945800", "NewName");

        assertEquals(Optional.empty(), storage.lookupXuidByName("OldName"));
        assertEquals(Optional.of("2535466392945800"), storage.lookupXuidByName("NewName"));
        assertEquals(Optional.of("NewName"), storage.lookupNameByXuid("2535466392945800"));
    }

    @Test
    void testOldNameTakenByAnotherPlayerIsNotDeleted() {
        storage.rememberIdentity("1111", "Shared");
        // İlk oyuncu tekrar giriş yapmadan önce ismi başka bir oyuncu alıyor
        storage.rememberIdentity("2222", "Shared");
        // İlk oyuncu isim değiştirdi; eski isim kaydı artık diğer oyuncuya ait
        storage.rememberIdentity("1111", "Fresh");

        assertEquals(Optional.of("2222"), storage.lookupXuidByName("Shared"));
        assertEquals(Optional.of("1111"), storage.lookupXuidByName("Fresh"));
    }

    @Test
    void testPersistsAcrossReopen() {
        storage.rememberIdentity("2535466392945800", "Lexa");
        storage.shutdown();

        storage = new AllayLevelDBPlayerIdentityStorage(tempDir.resolve("players-meta"));
        assertEquals(Optional.of("2535466392945800"), storage.lookupXuidByName("Lexa"));
    }
}
