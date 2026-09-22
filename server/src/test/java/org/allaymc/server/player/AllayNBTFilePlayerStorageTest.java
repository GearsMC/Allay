package org.allaymc.server.player;

import org.allaymc.api.player.PlayerData;
import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Clexa
 */
class AllayNBTFilePlayerStorageTest {

    private static final String XUID = "2535466392945800";

    @TempDir
    Path tempDir;

    @Test
    void testSaveReadRemoveKeyedByXuid() {
        var storage = new AllayNBTFilePlayerStorage(tempDir);
        var playerData = PlayerData.builder()
                .nbt(NbtMap.builder().putString("Test", "Value").build())
                .world("world")
                .dimension("minecraft:overworld")
                .build();

        assertFalse(storage.hasPlayerData(XUID));

        storage.savePlayerData(XUID, playerData);
        assertTrue(storage.hasPlayerData(XUID));
        assertTrue(Files.exists(tempDir.resolve(XUID + ".nbt")));

        var read = storage.readPlayerData(XUID);
        assertEquals("Value", read.getNbt().getString("Test"));
        assertEquals("world", read.getWorld());
        assertEquals("minecraft:overworld", read.getDimension());

        assertTrue(storage.removePlayerData(XUID));
        assertFalse(storage.hasPlayerData(XUID));
    }

    @Test
    void testStoredXuidsIgnoreLeftoverOldFiles() throws Exception {
        var storage = new AllayNBTFilePlayerStorage(tempDir);
        var playerData = PlayerData.builder()
                .nbt(NbtMap.builder().putString("Test", "Value").build())
                .world("world")
                .dimension("minecraft:overworld")
                .build();
        storage.savePlayerData(XUID, playerData);
        storage.savePlayerData("2535000000000001", playerData);
        // Yarım kalmış kayıttan kalan dosya ayrı bir oyuncu sayılmamalı.
        Files.writeString(tempDir.resolve("2535000000000002_old.nbt"), "");
        Files.writeString(tempDir.resolve("notes.txt"), "");

        assertEquals(Set.of(XUID, "2535000000000001"), storage.getStoredXuids());
    }
}
