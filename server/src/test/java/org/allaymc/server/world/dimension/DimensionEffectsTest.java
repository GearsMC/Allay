package org.allaymc.server.world.dimension;

import org.allaymc.api.player.FogIds;
import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(AllayTestExtension.class)
class DimensionEffectsTest {

    @Test
    void testFogIdentifier() {
        assertNull(DimensionEffects.fogIdentifier(DimensionTypes.OVERWORLD));
        assertEquals(FogIds.FOG_HELL, DimensionEffects.fogIdentifier(DimensionTypes.NETHER));
        assertEquals(FogIds.FOG_THE_END, DimensionEffects.fogIdentifier(DimensionTypes.THE_END));
        assertNull(DimensionEffects.fogIdentifier(null));
    }

    @Test
    void testDefaultBiome() {
        assertEquals(BiomeTypes.PLAINS, DimensionEffects.defaultBiome(DimensionTypes.OVERWORLD));
        assertEquals(BiomeTypes.HELL, DimensionEffects.defaultBiome(DimensionTypes.NETHER));
        assertEquals(BiomeTypes.THE_END, DimensionEffects.defaultBiome(DimensionTypes.THE_END));
        assertEquals(BiomeTypes.PLAINS, DimensionEffects.defaultBiome(null));
    }
}
