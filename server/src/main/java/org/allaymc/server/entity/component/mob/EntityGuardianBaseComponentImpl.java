package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.joml.Vector3d;

public class EntityGuardianBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected static final int CURSE_INTERVAL = 1200;
    protected static final double CURSE_RANGE = 50;
    protected static final int CURSE_DURATION = 6000;
    protected static final int CURSE_AMPLIFIER = 2;

    protected final boolean elder;

    protected volatile long laserTargetUniqueId = -1L;
    protected long broadcastLaserTarget = -1L;

    public EntityGuardianBaseComponentImpl(EntityInitInfo initInfo, boolean elder) {
        super(initInfo, elder ? 1.99 : 0.85, elder ? 1.99 : 0.85);
        this.elder = elder;
    }

    public boolean isElder() {
        return elder;
    }

    public boolean isFiringLaser() {
        return laserTargetUniqueId != -1L;
    }

    public void setLaserTarget(Entity target) {
        this.laserTargetUniqueId = target == null ? -1L : target.getUniqueId().getLeastSignificantBits();
    }

    @EventHandler
    protected void onGuardianTick(CEntityTickEvent event) {
        var current = laserTargetUniqueId;
        if (current != broadcastLaserTarget) {
            broadcastLaserTarget = current;
            broadcastState();
        }

        if (elder && thisEntity.isAlive() && event.getCurrentTick() % CURSE_INTERVAL == 0) {
            curseNearbyPlayers();
        }
    }

    protected void curseNearbyPlayers() {
        var rangeSquared = CURSE_RANGE * CURSE_RANGE;
        for (var player : getDimension().getPlayers()) {
            EntityPlayer entity = player.getControlledEntity();
            if (entity == null || !entity.isAlive() || entity.getLocation().distanceSquared(location) > rangeSquared) {
                continue;
            }
            if (entity.getGameMode() != GameMode.SURVIVAL && entity.getGameMode() != GameMode.ADVENTURE) {
                continue;
            }
            var current = entity.getEffects().get(EffectTypes.MINING_FATIGUE);
            if (current != null && current.getAmplifier() >= CURSE_AMPLIFIER && current.getDuration() > CURSE_INTERVAL) {
                continue;
            }

            entity.addEffect(new EffectInstance(EffectTypes.MINING_FATIGUE, CURSE_AMPLIFIER, CURSE_DURATION, false, true));
            var loc = entity.getLocation();
            player.viewParticle(SimpleParticle.GUARDIAN_CURSE, new Vector3d(loc.x(), loc.y(), loc.z()));
        }
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        super.writeMetadata(metadata);
        metadata.setFlag(EntityFlag.ELDER, elder);
        metadata.put(EntityDataTypes.TARGET_EID, laserTargetUniqueId == -1L ? 0L : laserTargetUniqueId);
    }
}
