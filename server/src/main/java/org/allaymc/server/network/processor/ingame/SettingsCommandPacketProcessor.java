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
     * Ayarlar ekranindaki komut kutusundan gelen komutu isler.
     *
     * <p>{@code CommandRequestPacketProcessor} ile ayni sekilde {@link PlayerCommandEvent}
     * tetiklenir. Onceden bu yol olayi atliyordu; oyuncudan gelen ayni komut hangi pakete
     * bindigine gore farkli davraniyor, komutu kesen eklentiler bu kutudan yazilani
     * goremiyordu. GearsMC'de bunun somut sonucu bir moderasyon atlatmasiydi: ozel mesaj
     * kesicisi devreye girmedigi icin susturulmus bir oyuncu buradan {@code /msg}
     * gonderebiliyordu.</p>
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
