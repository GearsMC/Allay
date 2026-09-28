package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.ProtocolRegistry;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.joml.primitives.AABBd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(AllayTestExtension.class)
class HumanNpcBaseAabbTest {

    private static EntityPlayer createHumanNpc() {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        return EntityTypes.PLAYER.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(0, 64, 0)
                .build());
    }

    @Test
    void humanNpcKeepsTheDefaultPlayerBoxWithoutCustomBox() {
        var npc = createHumanNpc();
        var box = npc.getBaseAABB();
        assertEquals(0.6, box.maxX() - box.minX(), 1e-9);
        assertEquals(1.8, box.maxY() - box.minY(), 1e-9);
    }

    @Test
    void humanNpcUsesTheCustomBaseBox() {
        var npc = createHumanNpc();
        npc.setBaseAABB(new AABBd(-0.05, 0.0, -0.05, 0.05, 0.1, 0.05));
        npc.setSneaking(true);

        var box = npc.getBaseAABB();
        assertEquals(0.1, box.maxX() - box.minX(), 1e-9);
        assertEquals(0.1, box.maxY() - box.minY(), 1e-9);

        var encoder = ProtocolRegistry.getDefault().getLatest(ClientVariant.INTERNATIONAL).getEncoder();
        var addPlayer = assertInstanceOf(AddPlayerPacket.class, encoder.encodeEntitySpawn(npc));
        assertEquals(0.1f, (float) addPlayer.getMetadata().get(EntityDataTypes.WIDTH), 1e-6f);
        assertEquals(0.1f, (float) addPlayer.getMetadata().get(EntityDataTypes.HEIGHT), 1e-6f);
    }

    @Test
    void clearingTheCustomBoxRestoresThePlayerBox() {
        var npc = createHumanNpc();
        npc.setBaseAABB(new AABBd(-0.05, 0.0, -0.05, 0.05, 0.1, 0.05));
        npc.setBaseAABB(null);
        var box = npc.getBaseAABB();
        assertEquals(1.8, box.maxY() - box.minY(), 1e-9);
    }
}
