package org.allaymc.server.network.protocol.v2192;

import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.PacketEncoder;
import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v2169.Protocol_v2169;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v2192.Bedrock_v2192;

/**
 * Bedrock 1.26.50 için uluslararası protokol v2192.
 */
public class Protocol_v2192 extends Protocol_v2169 {

    public Protocol_v2192() {
        this(Bedrock_v2192.CODEC, ClientVariant.INTERNATIONAL);
    }

    protected Protocol_v2192(BedrockCodec codec, ClientVariant variant) {
        super(codec, variant);
    }

    /**
     * Resmi blok paleti; 2192 ve 2193 protokolleri bu paleti kullanır.
     */
    @Override
    protected String getBlockPaletteResource() {
        return "protocol_palettes/1_26_50.nbt";
    }

    @Override
    protected PacketEncoder createEncoder(ProtocolData data) {
        return new PacketEncoder_v2192(data);
    }
}
