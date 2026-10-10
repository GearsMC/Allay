package org.allaymc.server.entity.effect;

import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.utils.identifier.Identifier;

import java.awt.*;

/**
 * @author daoge_cmd
 */
public class EffectAbsorptionType extends AbstractEffectType {
    public EffectAbsorptionType() {
        super(22, new Identifier("minecraft:absorption"), new Color(0x2552a5));
    }

    @Override
    public void onAdd(EntityLiving entity, EffectInstance effectInstance) {
        var absorption = effectInstance.getLevel() * 4;
        if (absorption > entity.getAbsorption()) {
            entity.setAbsorption(absorption);
        }
    }

    @Override
    public void onRemove(EntityLiving entity, EffectInstance effectInstance) {
        entity.setAbsorption(0);
    }
}
