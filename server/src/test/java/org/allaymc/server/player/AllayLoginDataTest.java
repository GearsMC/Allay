package org.allaymc.server.player;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GearsMC fork: UUID her zaman xuid'den (yoksa isimden) deterministik uretilir;
 * upstream'in "authed ise identity korunur" kurali burada gecerli degildir.
 */
class AllayLoginDataTest {

    @Test
    void testXuidBasedIdentityIsStable() {
        var expected = UUID.nameUUIDFromBytes(
                "xuid:123456789".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(expected, AllayLoginData.resolveUuid("123456789", "Player"));
        // xuid varken oyuncu adi degisse de kimlik degismez
        assertEquals(expected, AllayLoginData.resolveUuid("123456789", "OtherName"));
    }

    @Test
    void testOfflineIdentityIsStableAndSeparatedFromOnlineIdentity() {
        var first = AllayLoginData.resolveUuid(null, "123456789");
        var second = AllayLoginData.resolveUuid("", "123456789");
        var online = AllayLoginData.resolveUuid("123456789", "123456789");

        assertEquals(first, second);
        assertEquals(
                UUID.nameUUIDFromBytes("xname:123456789".getBytes(StandardCharsets.UTF_8)),
                first
        );
        assertNotEquals(online, first);
        assertNotEquals(first, AllayLoginData.resolveUuid(null, "OtherPlayer"));
    }

    @Test
    void testOfflineIdentityRejectsBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AllayLoginData.resolveUuid(null, " ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AllayLoginData.resolveUuid("", null)
        );
    }
}
