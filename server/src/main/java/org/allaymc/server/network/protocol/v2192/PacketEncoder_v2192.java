package org.allaymc.server.network.protocol.v2192;

import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v2169.PacketEncoder_v2169;

/**
 * v2192 (1.26.50) paket kodlayıcısı.
 *
 * <p>1.26.50'nin tel değişikliklerinin hepsi kodek serializer'larında: PlayerAuthInput, envanter
 * işlemi ve eşya yanıtındaki çift boolean sarmalayıcıları kalktı, BossEvent oyuncu kimliğini artık
 * taşımıyor, alt chunk yükseklik haritası parçalı yazılıyor. Sunucunun kurduğu paketlere gelen yeni
 * alanlar ({@code PlaySoundPacket}, {@code CameraPreset}, {@code PrimitiveText}) varsayılan
 * değerleriyle geçerli olduğu için v2169 davranışı olduğu gibi devralınır.</p>
 *
 * <p>Bilinen sınır: {@code DimensionDefinition}'da {@code packId} (v2168) ve {@code defaultBiome}
 * (v2192) boş gönderilemiyor, encoder ise ikisini de doldurmuyor. Vanilla dışı ya da varsayılan
 * sınırları değiştirilmiş bir boyut tipi kaydedilirse {@code DimensionDataPacket} v2168'den beri
 * kodlanamaz; bu sürüme özgü değil.</p>
 */
public class PacketEncoder_v2192 extends PacketEncoder_v2169 {

    public PacketEncoder_v2192(ProtocolData data) {
        super(data);
    }
}
