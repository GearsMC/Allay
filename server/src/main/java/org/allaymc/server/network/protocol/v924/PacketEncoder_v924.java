package org.allaymc.server.network.protocol.v924;

import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v898.PacketEncoder_v898;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class PacketEncoder_v924 extends PacketEncoder_v898 {
    public PacketEncoder_v924(ProtocolData data) {
        super(data);
    }

    /**
     * v924'un ses tablosu alet seviyesine ozel mizrak seslerini ekledi
     * (bkz. {@code Bedrock_v924.SOUND_EVENTS}).
     */
    @Override
    protected boolean supportsSoundEvent(SoundEvent soundEvent) {
        return switch (soundEvent) {
            case STONE_SPEAR_ATTACK_HIT, STONE_SPEAR_ATTACK_MISS, STONE_SPEAR_USE,
                 COPPER_SPEAR_ATTACK_HIT, COPPER_SPEAR_ATTACK_MISS, COPPER_SPEAR_USE,
                 IRON_SPEAR_ATTACK_HIT, IRON_SPEAR_ATTACK_MISS, IRON_SPEAR_USE,
                 GOLDEN_SPEAR_ATTACK_HIT, GOLDEN_SPEAR_ATTACK_MISS, GOLDEN_SPEAR_USE,
                 DIAMOND_SPEAR_ATTACK_HIT, DIAMOND_SPEAR_ATTACK_MISS, DIAMOND_SPEAR_USE,
                 NETHERITE_SPEAR_ATTACK_HIT, NETHERITE_SPEAR_ATTACK_MISS,
                 NETHERITE_SPEAR_USE -> true;
            default -> super.supportsSoundEvent(soundEvent);
        };
    }

    @Override
    public Collection<VoxelShapesPacket> encodeVoxelShapes() {
        var packet = new VoxelShapesPacket();
        packet.setNameMap(new HashMap<>());
        packet.setShapes(new ArrayList<>());
        packet.setCustomShapeCount(0);
        return List.of(packet);
    }
}
