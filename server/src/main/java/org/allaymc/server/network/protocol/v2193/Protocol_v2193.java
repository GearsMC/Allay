package org.allaymc.server.network.protocol.v2193;

import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.PacketEncoder;
import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v2192.Protocol_v2192;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v2193.Bedrock_v2193;

/**
 * Bedrock 1.26.50 tam sürümü için uluslararası protokol v2193.
 *
 * <p>v2192 aynı sürümün son önizlemesiydi. Tam sürüm istemcisi 2193 bildirir ve kayıt defteri numarayı birebir
 * eşlediği için bu sınıf olmadan istemci "sunucu eski" uyarısıyla geri çevrilir. Tel biçimi v2192 ile aynı.</p>
 */
public class Protocol_v2193 extends Protocol_v2192 {

    public Protocol_v2193() {
        this(Bedrock_v2193.CODEC, ClientVariant.INTERNATIONAL);
    }

    protected Protocol_v2193(BedrockCodec codec, ClientVariant variant) {
        super(codec, variant);
    }

    @Override
    protected PacketEncoder createEncoder(ProtocolData data) {
        return new PacketEncoder_v2193(data);
    }
}
