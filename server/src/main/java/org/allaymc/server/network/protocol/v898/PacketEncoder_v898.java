package org.allaymc.server.network.protocol.v898;

import org.allaymc.server.network.protocol.ProtocolData;
import org.allaymc.server.network.protocol.v860.PacketEncoder_v860;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

public class PacketEncoder_v898 extends PacketEncoder_v860 {
    public PacketEncoder_v898(ProtocolData data) {
        super(data);
    }

    /**
     * v898'in ses tablosuna Lunge buyusu ile ahsap/jenerik mizrak sesleri eklendi
     * (bkz. {@code Bedrock_v898.SOUND_EVENTS}). Alet seviyesine ozel mizrak
     * sesleri hala yok; onlari v924 aciyor.
     */
    @Override
    protected boolean supportsSoundEvent(SoundEvent soundEvent) {
        return switch (soundEvent) {
            case LUNGE_1, LUNGE_2, LUNGE_3,
                 SPEAR_ATTACK_HIT, SPEAR_ATTACK_MISS, SPEAR_USE,
                 WOODEN_SPEAR_ATTACK_HIT, WOODEN_SPEAR_ATTACK_MISS, WOODEN_SPEAR_USE -> true;
            default -> super.supportsSoundEvent(soundEvent);
        };
    }
}
