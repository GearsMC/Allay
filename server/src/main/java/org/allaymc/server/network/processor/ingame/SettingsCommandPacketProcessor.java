package org.allaymc.server.network.processor.ingame;

import org.allaymc.api.eventbus.event.player.PlayerCommandEvent;
import org.allaymc.api.player.Player;
import org.allaymc.api.registry.Registries;
import org.allaymc.server.network.processor.PacketProcessor;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.SettingsCommandPacket;

/**
 * @author daoge_cmd
 */
public class SettingsCommandPacketProcessor extends PacketProcessor<SettingsCommandPacket> {
    /**
     * Istemcinin ayar degisikliginden urettigi komutu isler.
     *
     * <p>Bu paket oyuncunun yazdigi bir komut DEGILDIR: istemci, ayarlardaki bir dugme
     * degistiginde karsiligi olan komutu kendisi uretip yollar (ornegin "Koordinatlari Goster"
     * acilinca {@code /gamerule showcoordinates true}).</p>
     *
     * <p>Onceden bu yol {@link PlayerCommandEvent} tetiklemeden komutu dogrudan
     * calistiriyordu; komut dinleyen ya da kesen eklentiler ayar kaynakli komutlari hic
     * gormuyordu. Artik {@code CommandRequestPacketProcessor} ile ayni akis kullaniliyor,
     * boylece oyuncu adina calisan her komut tek bir olaydan gecer.</p>
     */
    @Override
    public void handleSync(Player player, SettingsCommandPacket packet, long receiveTime) {
        var command = packet.getCommand().substring(1);

        var event = new PlayerCommandEvent(player.getControlledEntity(), command);
        if (event.call()) {
            Registries.COMMANDS.execute(player.getControlledEntity(), event.getCommand());
        }
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.SETTINGS_COMMAND;
    }
}
