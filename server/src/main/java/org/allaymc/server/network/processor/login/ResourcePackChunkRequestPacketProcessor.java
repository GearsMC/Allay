package org.allaymc.server.network.processor.login;

import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.pack.Pack;
import org.allaymc.api.player.Player;
import org.allaymc.api.registry.Registries;
import org.allaymc.server.AllayServer;
import org.allaymc.server.network.processor.ingame.ILoginPacketProcessor;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackChunkRequestPacket;

import static org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType.RESOURCE_PACK_CHUNK_REQUEST;

/**
 * @author daoge_cmd
 */
@Slf4j
public class ResourcePackChunkRequestPacketProcessor extends ILoginPacketProcessor<ResourcePackChunkRequestPacket> {
    @Override
    public void handle(Player player, ResourcePackChunkRequestPacket packet) {
        var pack = Registries.PACKS.get(packet.getPackId());
        if (pack == null) {
            log.warn("Chunk request for unknown pack {} index {}", packet.getPackId(), packet.getChunkIndex());
            player.disconnect(TrKeys.MC_DISCONNECTIONSCREEN_RESOURCEPACK);
            return;
        }

        log.debug("Sending pack chunk {} index {} to {}", pack.getName(), packet.getChunkIndex(), player.getOriginName());
        player.sendPacketImmediately(getChunkDataPacket(pack, packet.getChunkIndex()));
    }

    public ResourcePackChunkDataPacket getChunkDataPacket(Pack pack, int chunkIndex) {
        var chunkSize = AllayServer.getSettings().resourcePackSettings().maxChunkSize() * 1024;
        var packet = new ResourcePackChunkDataPacket();
        packet.setPackId(pack.getId());
        packet.setPackVersion(pack.getStringVersion());
        packet.setChunkIndex(chunkIndex);
        packet.setData(Unpooled.copiedBuffer(pack.getChunk(Math.multiplyExact(chunkSize, chunkIndex), chunkSize)));
        packet.setProgress((long) chunkSize * chunkIndex);
        return packet;
    }

    @Override
    public BedrockPacketType getPacketType() {
        return RESOURCE_PACK_CHUNK_REQUEST;
    }
}
