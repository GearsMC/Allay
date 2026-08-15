package org.allaymc.server.network.protocol.v2168;

import org.allaymc.server.network.protocol.ClientVariant;
import org.allaymc.server.network.protocol.PacketEncoder;
import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v1001.Protocol_v1001;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168_hotfix4;

/**
 * GearsMC fork: 1.26.40-1.26.44 (v2168) protokol destegi.
 *
 * <p>Upstream multi-version mimarisi v1001'e (1.26.30) kadar geliyor; sunucumuz v2168
 * konusan istemcilere hizmet ettigi icin bu kayit fork'ta eklendi. Protokol
 * kutuphanesinin v2168 kodeki de fork'ta portlandi.</p>
 *
 * <p><b>Neden hotfix4 kodeki:</b> Mojang 1.26.44'u protokol numarasini degistirmeden
 * yayinladi — hem 1.26.40 hem 1.26.44 istemcisi 2168 bildiriyor, yani ikisi
 * negotiation sirasinda ayirt edilemiyor ve tek kodek secmek zorundayiz.
 * Iki kodek arasindaki tek tel farki {@code SetScorePacket}'in {@code INVALID}
 * skorer dalinda fazladan bir boolean; {@code AllayPlayer.toNetworkScoreInfo}
 * yalnizca ENTITY/PLAYER/FAKE uretiyor, dolayisiyla o dala hic girilmiyor.
 * Paket sunucudan istemciye gittigi icin cozme yolu da kullanilmiyor. Boylece
 * hotfix4 bizim icin davranis degistirmiyor, buna karsilik bildirilen surum
 * dizesi ("1.26.44") guncel istemciyle ortusuyor.</p>
 *
 * <p>Oyun verisi hala 1.26.30: {@code data/resources} altindaki statik dosyalar
 * yenilenmedigi icin v2168 istemcileri baglanir ama 1.26.30 blok paleti, esyalari
 * ve tarifleriyle oynar. Bunu duzeltmek {@code ProtocolInfo.FEATURE_VERSION}
 * sabitini degistirmekle olmaz — o sabit veriyi secmez; bkz.
 * {@code data/resources/unpacked/staging-1.26.40/README.md}.</p>
 */
public class Protocol_v2168 extends Protocol_v1001 {

    public Protocol_v2168() {
        this(Bedrock_v2168_hotfix4.CODEC, ClientVariant.INTERNATIONAL);
    }

    protected Protocol_v2168(BedrockCodec codec, ClientVariant variant) {
        super(codec, variant);
    }

    @Override
    protected PacketEncoder createEncoder(ProtocolData data) {
        return new PacketEncoder_v2168(data);
    }
}
