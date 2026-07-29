package org.allaymc.server.network.protocol.v2168;

import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.PacketEncoder;
import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v1001.Protocol_v1001;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168;

/**
 * GearsMC fork: 1.26.40 (v2168) protokol destegi.
 *
 * <p>Upstream multi-version mimarisi v1001'e (1.26.30) kadar geliyor; sunucumuz v2168
 * konusan istemcilere hizmet ettigi icin bu kayit fork'ta eklendi. Protokol
 * kutuphanesinin v2168 kodeki de fork'ta portlandi.</p>
 *
 * <p>Oyun verisi hala 1.26.30 — bkz. {@code ProtocolInfo.FEATURE_VERSION}. v2168
 * istemcileri baglanir ve 1.26.30 icerigi alir.</p>
 */
public class Protocol_v2168 extends Protocol_v1001 {

    public Protocol_v2168() {
        this(Bedrock_v2168.CODEC, ClientVariant.INTERNATIONAL);
    }

    protected Protocol_v2168(BedrockCodec codec, ClientVariant variant) {
        super(codec, variant);
    }

    @Override
    protected PacketEncoder createEncoder(ProtocolData data) {
        return new PacketEncoder_v2168(data);
    }
}
