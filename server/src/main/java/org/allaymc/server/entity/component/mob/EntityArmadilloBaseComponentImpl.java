package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.property.enums.ArmadilloState;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

import java.util.concurrent.ThreadLocalRandom;

public class EntityArmadilloBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected static final double THREAT_RANGE = 7;
    protected static final int HURT_THREAT_TICKS = 60;
    protected static final int CALM_TICKS_TO_UNROLL = 60;
    protected static final int UNROLL_TICKS = 20;

    @Dependency
    protected EntityLivingComponent livingComponent;

    protected volatile boolean rolled;
    protected int calmTicks;
    protected int unrollTicks;

    public EntityArmadilloBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.7, 0.65);
        babyScale(0.6f);
        periodicDrop(() -> ThreadLocalRandom.current().nextInt(6000, 10801),
                () -> ItemTypes.ARMADILLO_SCUTE.createItemStack(1), SoundNames.MOB_ARMADILLO_SCUTE_DROP);
    }

    public boolean isRolled() {
        return rolled;
    }

    @EventHandler
    protected void onArmadilloTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive()) {
            return;
        }

        var threatened = isThreatened();
        if (threatened) {
            calmTicks = 0;
            unrollTicks = 0;
            if (!rolled) {
                rolled = true;
                setState(ArmadilloState.ROLLED_UP);
                getDimension().addSound(location, new CustomSound(SoundNames.MOB_ARMADILLO_ROLL));
            }
            return;
        }

        if (!rolled) {
            return;
        }

        if (unrollTicks > 0) {
            if (--unrollTicks == 0) {
                rolled = false;
                setState(ArmadilloState.UNROLLED);
            }
            return;
        }

        if (++calmTicks >= CALM_TICKS_TO_UNROLL) {
            unrollTicks = UNROLL_TICKS;
            setState(ArmadilloState.ROLLED_UP_UNROLLING);
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_ARMADILLO_UNROLL_FINISH));
        }
    }

    protected void setState(ArmadilloState state) {
        setPropertyValue(EntityPropertyTypes.ARMADILLO_STATE, state);
    }

    protected boolean isThreatened() {
        if (thisEntity.getTick() - livingComponent.getLastDamageTime() <= HURT_THREAT_TICKS
                && livingComponent.getLastDamage() != null) {
            return true;
        }

        var rangeSquared = THREAT_RANGE * THREAT_RANGE;
        for (var controller : getDimension().getPlayers()) {
            var player = controller.getControlledEntity();
            if (player == null || !player.isAlive() || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (player.isSprinting() && player.getLocation().distanceSquared(location) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }
}
