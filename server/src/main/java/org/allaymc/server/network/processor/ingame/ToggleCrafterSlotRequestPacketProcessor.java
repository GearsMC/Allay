package org.allaymc.server.network.processor.ingame;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.interfaces.BlockEntityCrafter;
import org.allaymc.api.container.interfaces.CrafterContainer;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.player.Player;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.network.processor.PacketProcessor;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.ToggleCrafterSlotRequestPacket;
import org.joml.Vector3i;

@Slf4j
public class ToggleCrafterSlotRequestPacketProcessor extends PacketProcessor<ToggleCrafterSlotRequestPacket> {

    protected static final double MAX_INTERACTION_DISTANCE = 6.0;

    @Override
    public void handleSync(Player player, ToggleCrafterSlotRequestPacket packet, long receiveTime) {
        var entity = player.getControlledEntity();
        var dimension = entity.getDimension();
        var blockPos = packet.getBlockPosition();
        int x = blockPos.getX(), y = blockPos.getY(), z = blockPos.getZ();

        if (dimension.getBlockState(x, y, z).getBlockType() != BlockTypes.CRAFTER) {
            log.warn("Player {} tried to toggle a slot of a non-crafter block at ({}, {}, {})", player.getOriginName(), x, y, z);
            return;
        }

        if (entity.getLocation().distanceSquared(x + 0.5, y + 0.5, z + 0.5) > MAX_INTERACTION_DISTANCE * MAX_INTERACTION_DISTANCE) {
            log.warn("Player {} is too far from the crafter at ({}, {}, {})", player.getOriginName(), x, y, z);
            return;
        }

        if (!(dimension.getBlockEntity(x, y, z) instanceof BlockEntityCrafter crafter)) {
            return;
        }

        int slot = packet.getSlot();
        if (slot < 0 || slot >= CrafterContainer.SIZE) {
            log.warn("Player {} sent an out of range crafter slot index {}", player.getOriginName(), slot);
            return;
        }

        CrafterContainer container = crafter.getContainer();
        if (!player.getOpenedContainers().contains(container)) {
            log.warn("Player {} tried to toggle a crafter slot without opening the crafter", player.getOriginName());
            return;
        }

        container.setSlotDisabled(slot, packet.isDisabled());
        crafter.syncSlotMaskToViewers();
        crafter.syncRecipePreviewToViewers();

        if (packet.isDisabled()) {
            dimension.addSound(MathUtils.center(new Vector3i(x, y, z)), new CustomSound(SoundNames.CRAFTER_DISABLE_SLOT));
        }
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.TOGGLE_CRAFTER_SLOT_REQUEST;
    }
}
