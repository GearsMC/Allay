package org.allaymc.server.network.processor.login;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.player.Player;
import org.allaymc.api.registry.Registries;
import org.allaymc.server.AllayServer;
import org.allaymc.server.network.processor.ingame.ILoginPacketProcessor;
import org.allaymc.server.player.AllayPlayer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkRequestPacket;

import static org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType.RESOURCE_PACK_CHUNK_REQUEST;

/**
 * @author daoge_cmd
 */
@Slf4j
public class ResourcePackChunkRequestPacketProcessor extends ILoginPacketProcessor<ResourcePackChunkRequestPacket> {
    @Override
    public void handle(Player player, ResourcePackChunkRequestPacket packet) {
        var allayPlayer = (AllayPlayer) player;
        var protocol = allayPlayer.getProtocol();
        var pack = Registries.PACKS.get(packet.getPackId());
        if (pack == null) {
            log.warn("Chunk request for unknown pack {} index {}", packet.getPackId(), packet.getChunkIndex());
            player.disconnect(TrKeys.MC_DISCONNECTIONSCREEN_RESOURCEPACK);
            return;
        }

        log.debug("Sending pack chunk {} index {} to {}", pack.getName(), packet.getChunkIndex(), player.getOriginName());
        int chunkSize = AllayServer.getSettings()
                .resourcePackSettings()
                .maxChunkSize() * 1024;
        try {
            // Fork: sendPacketImmediately, not sendPacket. Pack chunks must not queue behind
            // ordinary traffic — a delayed chunk stalls the whole download on the client.
            allayPlayer.sendPacketImmediately(protocol.getEncoder().encodeResourcePackChunkData(
                    pack,
                    packet.getChunkIndex(),
                    chunkSize
            ));
        } catch (IllegalArgumentException ignored) {
            player.disconnect(TrKeys.MC_DISCONNECTIONSCREEN_RESOURCEPACK);
        }
    }

    @Override
    public BedrockPacketType getPacketType() {
        return RESOURCE_PACK_CHUNK_REQUEST;
    }
}
