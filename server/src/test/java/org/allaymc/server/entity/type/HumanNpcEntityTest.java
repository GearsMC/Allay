package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.player.Skin;
import org.allaymc.api.server.Server;
import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.ProtocolRegistry;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Kontrolcusu olmayan bir oyuncu varliginin insan NPC'si olarak
 * kullanilabildigini dogrular.
 *
 * <p>Ozel derili insan NPC'si icin motorda ayri bir varlik turu yok; yol,
 * {@code EntityTypes.PLAYER}'i ag baglantisi olmadan dogurmaktan geciyor.
 * Bunun calismasi iki sarta bagli: varligin {@code isActualPlayer()} degeri
 * {@code false} olmali (deri gonderimi o dala bakiyor) ve dogurma paketi
 * {@link AddPlayerPacket} olmali — aksi halde istemci insan degil jenerik bir
 * varlik cizer.</p>
 */
@ExtendWith(AllayTestExtension.class)
class HumanNpcEntityTest {

    private static EntityPlayer createHumanNpc() {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        return EntityTypes.PLAYER.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(0, 64, 0)
                .build());
    }

    @Test
    void playerEntityWithoutControllerIsNotAnActualPlayer() {
        assertFalse(createHumanNpc().isActualPlayer(),
                "kontrolcusuz oyuncu varligi gercek oyuncu sayilmamali, yoksa deri sahte oyuncu dalindan gitmez");
    }

    @Test
    void humanNpcKeepsTheSkinItIsGiven() {
        var npc = createHumanNpc();
        var skin = Skin.builder()
                .skinId("gears_test_skin")
                .skinResourcePatch("{\"geometry\":{\"default\":\"geometry.humanoid.custom\"}}")
                .skinData(Skin.ImageData.from(new byte[64 * 64 * 4]))
                .animations(List.of())
                .capeData(Skin.ImageData.EMPTY)
                .skinGeometry("{}")
                .animationData("")
                .armSize(Skin.ARM_SIZE_WIDE)
                .personaPieces(List.of())
                .pieceTintColors(List.of())
                .build();

        npc.setSkin(skin);

        assertEquals(skin, npc.getSkin());
    }

    @Test
    void humanNpcSpawnsWithAddPlayerPacket() {
        var npc = createHumanNpc();
        npc.setNameTag("Rehber");

        var encoder = ProtocolRegistry.getDefault().getLatest(ClientVariant.INTERNATIONAL).getEncoder();
        var packet = encoder.encodeEntitySpawn(npc);

        var addPlayer = assertInstanceOf(AddPlayerPacket.class, packet,
                "insan NPC'si AddPlayerPacket ile dogurulmali");
        assertEquals("Rehber", addPlayer.getUsername(), "ad etiketi istemciye kullanici adi olarak gider");
    }
}
