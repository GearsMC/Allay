package org.allaymc.server.network.protocol.v1001;

import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v975.PacketEncoder_v975;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

public class PacketEncoder_v1001 extends PacketEncoder_v975 {
    public PacketEncoder_v1001(ProtocolData data) {
        super(data);
    }

    @Override
    protected boolean supportsSoundEvent(SoundEvent soundEvent) {
        return switch (soundEvent) {
            case GEYSER_ERUPTION_START, GEYSER_ERUPTION_ACTIVE,
                 GEYSER_CONTINUOUS_ERUPTION_START, GEYSER_CONTINUOUS_ERUPTION_ACTIVE -> true;
            default -> super.supportsSoundEvent(soundEvent);
        };
    }
}
