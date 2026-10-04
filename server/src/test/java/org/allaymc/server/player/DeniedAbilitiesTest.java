package org.allaymc.server.player;

import org.allaymc.api.player.PlayerAbility;
import org.allaymc.server.network.AllayNetworkInterface;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Oturumluk yetenek engelleri kalıcı yetenekleri değiştirmez. Eskiden ada koruması kalıcı
 * {@code PLACE_BLOCK}/{@code BREAK_BLOCK}'u kapatıyordu; geri açma adımı kaçınca kayıp oyuncu
 * verisine yazılıp kalıcı oluyordu.
 */
@ExtendWith(AllayTestExtension.class)
class DeniedAbilitiesTest {

    private static AllayPlayer newMember() {
        var player = new AllayPlayer(mock(BedrockServerSession.class), mock(AllayNetworkInterface.class)) {
            @Override
            protected void onDisconnect(String disconnectReason) {
            }

            @Override
            public String getXuid() {
                return "denied-abilities-test";
            }
        };
        player.setAbilities(AllayPlayer.abilitiesFromPermission(PlayerPermission.MEMBER));
        return player;
    }

    @Test
    void denyingBlocksEffectiveAbilityButKeepsPersistentOne() {
        var player = newMember();
        assertTrue(player.canPlaceBlocks());
        assertTrue(player.canBreakBlocks());

        player.setDeniedAbilities(Set.of(PlayerAbility.PLACE_BLOCK, PlayerAbility.BREAK_BLOCK));

        assertFalse(player.canPlaceBlocks());
        assertFalse(player.canBreakBlocks());
        assertTrue(player.canInteractWithBlocks());
        // Kaydedilen küme (PlayerData buradan yazar) değişmedi.
        assertTrue(player.hasAbility(PlayerAbility.PLACE_BLOCK));
        assertTrue(player.getAbilities().contains(PlayerAbility.BREAK_BLOCK));
    }

    @Test
    void networkAbilitiesHideDeniedOnes() {
        var player = newMember();
        player.setDeniedAbilities(Set.of(PlayerAbility.PLACE_BLOCK));

        var network = player.calculateAbilities(player);
        assertFalse(network.contains(Ability.BUILD));
        assertTrue(network.contains(Ability.MINE));
        assertEquals(PlayerPermission.CUSTOM, player.calculatePlayerPermission(player));
    }

    @Test
    void clearingDenialsRestoresWithoutTouchingPersistentAbilities() {
        var player = newMember();
        var before = EnumSet.copyOf(player.getAbilities());

        player.setDeniedAbilities(Set.of(PlayerAbility.PLACE_BLOCK, PlayerAbility.BREAK_BLOCK));
        player.setDeniedAbilities(Set.of());

        assertTrue(player.getDeniedAbilities().isEmpty());
        assertTrue(player.canPlaceBlocks());
        assertTrue(player.canBreakBlocks());
        assertEquals(before, player.getAbilities());
        assertEquals(PlayerPermission.MEMBER, player.calculatePlayerPermission(player));
    }

    @Test
    void nullClearsDenials() {
        var player = newMember();
        player.setDeniedAbilities(Set.of(PlayerAbility.PLACE_BLOCK));
        player.setDeniedAbilities(null);
        assertTrue(player.getDeniedAbilities().isEmpty());
        assertTrue(player.canPlaceBlocks());
    }
}
