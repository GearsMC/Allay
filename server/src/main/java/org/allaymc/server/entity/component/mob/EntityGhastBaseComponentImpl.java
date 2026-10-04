package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

public class EntityGhastBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected volatile boolean charging;
    protected boolean broadcastCharging;

    public EntityGhastBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 4.0, 4.0);
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    @EventHandler
    protected void onGhastChargeTick(CEntityTickEvent event) {
        var current = charging;
        if (current == broadcastCharging) {
            return;
        }

        broadcastCharging = current;
        broadcastState();
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        super.writeMetadata(metadata);
        metadata.setFlag(EntityFlag.FIRE_IMMUNE, true);
        metadata.setFlag(EntityFlag.CHARGED, charging);
        metadata.put(EntityDataTypes.CHARGE_AMOUNT, (byte) (charging ? 1 : 0));
    }
}
