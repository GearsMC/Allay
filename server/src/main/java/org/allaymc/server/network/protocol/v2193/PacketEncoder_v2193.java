package org.allaymc.server.network.protocol.v2193;

import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v2192.PacketEncoder_v2192;

/**
 * v2193 (1.26.50 tam sürümü) paket kodlayıcısı.
 *
 * <p>Tam sürüm önizlemeyle (v2192) aynı tel biçimini kullandığı için kodlayıcı değişmeden devralınır.</p>
 */
public class PacketEncoder_v2193 extends PacketEncoder_v2192 {

    public PacketEncoder_v2193(ProtocolData data) {
        super(data);
    }
}
