package org.allaymc.server.network.protocol;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.action.SimpleEntityAction;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AllayTestExtension.class)
class ServerSwingArmEncoderTest {

    private static EntityPlayer createPlayerEntity() {
        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        return EntityTypes.PLAYER.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(0, 64, 0)
                .build());
    }

    @Test
    void serverSwingReachesTheSwingingPlayerItself() {
        var player = createPlayerEntity();
        for (var protocol : ProtocolRegistry.getDefault().getProtocols()) {
            var packets = protocol.getEncoder().encodeEntityAction(player, SimpleEntityAction.SERVER_SWING_ARM, true);
            assertEquals(1, packets.size(), protocol::toString);
            var event = assertInstanceOf(EntityEventPacket.class, packets.iterator().next(), protocol::toString);
            assertEquals(EntityEventType.ATTACK_START, event.getType(), protocol::toString);
            assertEquals(player.getRuntimeId(), event.getRuntimeEntityId(), protocol::toString);
        }
    }

    @Test
    void serverSwingReachesViewersAsAnimate() {
        var player = createPlayerEntity();
        for (var protocol : ProtocolRegistry.getDefault().getProtocols()) {
            var packets = protocol.getEncoder().encodeEntityAction(player, SimpleEntityAction.SERVER_SWING_ARM, false);
            assertEquals(1, packets.size(), protocol::toString);
            var animate = assertInstanceOf(AnimatePacket.class, packets.iterator().next(), protocol::toString);
            assertEquals(AnimatePacket.Action.SWING_ARM, animate.getAction(), protocol::toString);
            assertEquals(player.getRuntimeId(), animate.getRuntimeEntityId(), protocol::toString);
        }
    }

    @Test
    void clientSwingStillSkipsTheSwingingPlayer() {
        var player = createPlayerEntity();
        for (var protocol : ProtocolRegistry.getDefault().getProtocols()) {
            var packets = protocol.getEncoder().encodeEntityAction(player, SimpleEntityAction.SWING_ARM, true);
            assertTrue(packets.isEmpty(), protocol::toString);
        }
    }
}
