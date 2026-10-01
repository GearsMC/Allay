package org.allaymc.server.entity.ai.sensor;

import org.allaymc.api.entity.ai.memory.MemoryStorage;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.player.Player;
import org.allaymc.api.world.Dimension;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndermanStareSensorTest {

    private static EntityIntelligent enderman(Dimension dimension, MemoryStorage memory, Player player) {
        EntityIntelligent enderman = mock(EntityIntelligent.class);
        when(enderman.getLocation()).thenReturn(new Location3d(0, 64, 0, 0, 0, dimension));
        when(enderman.getEyeHeight()).thenReturn(2.55d);
        when(enderman.getDimension()).thenReturn(dimension);
        when(enderman.getMemoryStorage()).thenReturn(memory);
        when(dimension.getPlayers()).thenReturn(Set.of(player));
        return enderman;
    }

    private static Player player(double x, double z, double yaw, GameMode mode) {
        Player player = mock(Player.class);
        EntityPlayer entity = mock(EntityPlayer.class);
        when(player.getControlledEntity()).thenReturn(entity);
        when(entity.getLocation()).thenReturn(new Location3d(x, 64, z, yaw, 0, null));
        when(entity.getEyeHeight()).thenReturn(1.62d);
        when(entity.getGameMode()).thenReturn(mode);
        when(entity.getRuntimeId()).thenReturn(42L);
        return player;
    }

    @Test
    void playerStaringAtEndermanBecomesTarget() {
        Dimension dimension = mock(Dimension.class);
        MemoryStorage memory = mock(MemoryStorage.class);
        // Oyuncu +z yonunde 8 blokta; yaw 180 = -z yonune (endermana) bakar.
        Player staring = player(0, 8, 180, GameMode.SURVIVAL);
        EntityIntelligent enderman = enderman(dimension, memory, staring);

        new EndermanStareSensor(10).sense(enderman);

        verify(memory).put(MemoryTypes.ATTACK_TARGET, 42L);
    }

    @Test
    void playerLookingAwayOrCreativeOrFarIsIgnored() {
        Dimension dimension = mock(Dimension.class);
        MemoryStorage memory = mock(MemoryStorage.class);

        EntityIntelligent e1 = enderman(dimension, memory, player(0, 8, 0, GameMode.SURVIVAL));
        new EndermanStareSensor(10).sense(e1);

        EntityIntelligent e2 = enderman(dimension, memory, player(0, 8, 180, GameMode.CREATIVE));
        new EndermanStareSensor(10).sense(e2);

        EntityIntelligent e3 = enderman(dimension, memory, player(0, 20, 180, GameMode.SURVIVAL));
        new EndermanStareSensor(10).sense(e3);

        verify(memory, never()).put(any(), any());
    }
}
