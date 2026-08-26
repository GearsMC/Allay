package org.allaymc.server.network.protocol.v2169;

import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v2168.PacketEncoder_v2168;

/**
 * Packet encoder for protocol v2169.
 *
 * <p>Bedrock v2169 retains the v2168 wire mappings, so version-specific packet
 * construction is inherited unchanged.</p>
 */
public class PacketEncoder_v2169 extends PacketEncoder_v2168 {

    public PacketEncoder_v2169(ProtocolData data) {
        super(data);
    }
}
