package org.allaymc.server.command.selector;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Oyuncu kılığındaki NPC'ler (kontrolcüsü olmayan {@link EntityPlayer}) {@code /xp 10 @a} gibi
 * komutları çökertiyordu: seçici onları da yakalıyor, XP ayarlamak da olmayan istemciye paket
 * göndermeye çalışıp NPE atıyordu.
 */
@ExtendWith(AllayTestExtension.class)
class NpcPlayerSelectorTest {

    private static EntityPlayer createNpc() {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        return EntityTypes.PLAYER.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(0, 64, 0)
                .build());
    }

    @Test
    void npcIsNotAPlayerTarget() {
        assertFalse(AllayEntitySelectorAPI.isConnectedPlayer(createNpc()));
        assertFalse(AllayEntitySelectorAPI.isConnectedPlayer(mock(Entity.class)));

        var connected = mock(EntityPlayer.class);
        when(connected.isActualPlayer()).thenReturn(true);
        assertTrue(AllayEntitySelectorAPI.isConnectedPlayer(connected));
    }

    @Test
    void settingExperienceOnNpcDoesNotThrow() {
        var npc = createNpc();
        assertDoesNotThrow(() -> npc.setExperienceLevel(5));
        assertDoesNotThrow(() -> npc.setExperienceProgress(0.5f));
        assertEquals(5, npc.getExperienceLevel());
        assertEquals(0.5f, npc.getExperienceProgress());
        assertDoesNotThrow(() -> npc.addExperience(100));
    }
}
