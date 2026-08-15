package org.allaymc.api.entity.property.type;

import org.allaymc.api.annotation.MinecraftVersionSensitive;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.api.entity.property.enums.ClimateVariant;

@MinecraftVersionSensitive
public interface EntityPropertyTypes {
    EnumPropertyType<ClimateVariant> CLIMATE_VARIANT = EnumPropertyType.of("minecraft:climate_variant", ClimateVariant.class, ClimateVariant.TEMPERATE);

    /**
     * Sulfur kupunun emdigi bloga gore aldigi kisilik.
     *
     * <p>Kupun icinin nasil gorunecegine karar veren sey bu. Emilen blok istemciye bir blok durumu
     * olarak degil, bu property uzerinden bildiriliyor; property gonderilmezse kup her zaman bos
     * gorunur.</p>
     */
    EnumPropertyType<SulfurCubeArchetype> SULFUR_CUBE_ARCHETYPE = EnumPropertyType.of("minecraft:sulfur_cube_archetype", SulfurCubeArchetype.class, SulfurCubeArchetype.NONE);
}
