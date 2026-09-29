package org.allaymc.server;

import eu.okaeri.configs.ConfigManager;
import org.allaymc.server.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Varsayılan sunucu ayar dosyası: Türkçe açıklamalar doğru kodlanmış ve PHP değerleri yerinde. */
class ServerSettingsGeneratedFileTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedFileHasTurkishCommentsAndPhpDefaults() throws Exception {
        Path file = tempDir.resolve("server-settings.yml");
        var settings = ConfigManager.create(ServerSettings.class, Utils.createConfigInitializer(file));

        assertEquals("GearsMC Skyblock", settings.genericSettings().motd());
        assertEquals(100, settings.genericSettings().maxPlayerCount());

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("Sunucu listesinde görünen ad"), "Türkçe açıklama ve ö/ü karakterleri korunmalı");
        assertTrue(text.contains("motd: GearsMC Skyblock"));
        assertFalse(text.contains("Determines"), "İngilizce açıklama kalmamalı");
        assertFalse(text.contains("Usually only visible"));
    }
}
