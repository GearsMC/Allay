package org.allaymc.server.network.protocol;

import org.allaymc.api.camera.CameraInstruction;
import org.allaymc.server.network.protocol.v766.PacketEncoder_v766;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class CameraPacketEncoderTest {

    @Test
    void renkGecisiniAyriKameraTalimatinaKodlar() {
        var encoder = new PacketEncoder_v766(mock(ProtocolData.class));
        var packet = encoder.encodeCameraInstruction(
                CameraInstruction.fade(0.55f, 0.75f, 0.7f, 0.12f, 0.03f, 0.01f));

        assertNull(packet.getSetInstruction());
        assertNotNull(packet.getFadeInstruction());
        var time = packet.getFadeInstruction().getTimeData();
        assertEquals(0.55f, time.fadeInTime());
        assertEquals(0.75f, time.waitTime());
        assertEquals(0.7f, time.fadeOutTime());
        assertEquals(31, packet.getFadeInstruction().getColor().getRed());
        assertEquals(8, packet.getFadeInstruction().getColor().getGreen());
        assertEquals(3, packet.getFadeInstruction().getColor().getBlue());
    }
}
