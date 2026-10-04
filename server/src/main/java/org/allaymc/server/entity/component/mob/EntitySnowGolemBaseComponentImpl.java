package org.allaymc.server.entity.component.mob;

import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.data.Weather;
import org.allaymc.api.world.gamerule.GameRule;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

import java.util.List;

public class EntitySnowGolemBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected static final float SNOW_TEMPERATURE = 0.8f;
    protected static final float MELT_TEMPERATURE = 1.0f;

    @Dependency
    protected EntityLivingComponent livingComponent;

    public EntitySnowGolemBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.4, 1.8);
        shearable(SoundNames.MOB_SHEEP_SHEAR, () -> List.of(ItemTypes.CARVED_PUMPKIN.createItemStack(1)));
    }

    @EventHandler
    protected void onSnowGolemTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive()) {
            return;
        }

        var x = (int) Math.floor(location.x());
        var y = (int) Math.floor(location.y());
        var z = (int) Math.floor(location.z());
        var dimension = getDimension();
        var temperature = dimension.getBiome(x, y, z).getBiomeData().temperature();
        if (event.getCurrentTick() % 20 == 0) {
            var raining = thisEntity.getWorld().getWeather() != Weather.CLEAR && dimension.canPosSeeSky(x, y, z);
            if (thisEntity.isTouchingWater() || raining || temperature > MELT_TEMPERATURE) {
                livingComponent.attack(DamageContainer.magicEffect(1));
            }
        }

        if (temperature >= SNOW_TEMPERATURE
                || !thisEntity.getWorld().getWorldData().<Boolean>getGameRuleValue(GameRule.MOB_GRIEFING)) {
            return;
        }

        if (dimension.getBlockState(x, y, z).getBlockType() == BlockTypes.AIR
                && dimension.getBlockState(x, y - 1, z).getBlockStateData().hasCollision()) {
            dimension.setBlockState(x, y, z, BlockTypes.SNOW_LAYER.getDefaultState());
        }
    }
}
