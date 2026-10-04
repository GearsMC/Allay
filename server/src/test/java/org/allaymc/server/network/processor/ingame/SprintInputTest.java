package org.allaymc.server.network.processor.ingame;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Uçan (ya da uçabilen) oyuncu açlığından bağımsız koşabilir. Eskiden sunucu yalnızca açlığa bakıyor,
 * /fly kullanan aç oyuncuların koşusunu reddediyor ve aynı paketteki diğer girdileri de atlıyordu.
 */
class SprintInputTest {

    private final PlayerAuthInputPacketProcessor processor = new PlayerAuthInputPacketProcessor();
    private Player player;
    private EntityPlayer entity;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        entity = mock(EntityPlayer.class);
        when(player.getControlledEntity()).thenReturn(entity);
        when(player.getOriginName()).thenReturn("tester");
    }

    @Test
    void fedPlayerCanSprint() {
        when(entity.getFoodLevel()).thenReturn(7);
        assertTrue(PlayerAuthInputPacketProcessor.canStartSprinting(player, entity));
    }

    @Test
    void hungryWalkingPlayerCannotSprint() {
        when(entity.getFoodLevel()).thenReturn(6);
        assertFalse(PlayerAuthInputPacketProcessor.canStartSprinting(player, entity));
    }

    @Test
    void hungryPlayerWhoCanFlyCanSprint() {
        when(entity.getFoodLevel()).thenReturn(2);
        when(player.canFly()).thenReturn(true);
        assertTrue(PlayerAuthInputPacketProcessor.canStartSprinting(player, entity));
    }

    @Test
    void hungryFlyingPlayerCanSprint() {
        when(entity.getFoodLevel()).thenReturn(0);
        when(entity.isFlying()).thenReturn(true);
        assertTrue(PlayerAuthInputPacketProcessor.canStartSprinting(player, entity));
    }

    @Test
    void rejectedSprintDoesNotSkipOtherInputs() {
        when(entity.getFoodLevel()).thenReturn(3);

        processor.handleInputData(player, EnumSet.of(
                PlayerAuthInputData.START_SPRINTING,
                PlayerAuthInputData.START_SNEAKING,
                PlayerAuthInputData.START_GLIDING));

        verify(entity, never()).setSprinting(anyBoolean());
        verify(entity).setSneaking(true);
        verify(entity).setGliding(true);
    }

    @Test
    void flyingHungryPlayerStartsSprinting() {
        when(entity.getFoodLevel()).thenReturn(1);
        when(player.canFly()).thenReturn(true);

        processor.handleInputData(player, EnumSet.of(PlayerAuthInputData.START_SPRINTING));

        verify(entity).setSprinting(true);
    }
}
