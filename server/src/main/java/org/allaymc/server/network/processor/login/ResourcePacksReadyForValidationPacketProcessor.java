package org.allaymc.server.network.processor.login;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.player.Player;
import org.allaymc.server.network.processor.ingame.ILoginPacketProcessor;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksReadyForValidationPacket;

@Slf4j
public class ResourcePacksReadyForValidationPacketProcessor extends ILoginPacketProcessor<ResourcePacksReadyForValidationPacket> {
    @Override
    public void handle(Player player, ResourcePacksReadyForValidationPacket packet) {
        log.info("Client {} resource packs ready for validation", player.getOriginName());
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.RESOURCE_PACKS_READY_FOR_VALIDATION;
    }
}
