package org.allaymc.server.world.dimension;

import org.allaymc.api.player.FogIds;
import org.allaymc.api.world.biome.BiomeType;
import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.dimension.DimensionType;

public final class DimensionEffects {

    private DimensionEffects() {
    }

    public static String fogIdentifier(DimensionType dimensionType) {
        var dimensionId = DimensionId.fromDimensionType(dimensionType);
        if (dimensionId == DimensionId.NETHER) {
            return FogIds.FOG_HELL;
        }
        if (dimensionId == DimensionId.THE_END) {
            return FogIds.FOG_THE_END;
        }
        return null;
    }
    
    public static BiomeType defaultBiome(DimensionType dimensionType) {
        var dimensionId = DimensionId.fromDimensionType(dimensionType);
        if (dimensionId == DimensionId.NETHER) {
            return BiomeTypes.HELL;
        }
        if (dimensionId == DimensionId.THE_END) {
            return BiomeTypes.THE_END;
        }
        return BiomeTypes.PLAINS;
    }
}
