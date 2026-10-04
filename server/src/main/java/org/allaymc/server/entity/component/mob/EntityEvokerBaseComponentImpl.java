package org.allaymc.server.entity.component.mob;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

public class EntityEvokerBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected volatile int spellColor;
    protected volatile boolean casting;
    protected boolean broadcastCasting;

    @Getter
    @Setter
    protected volatile long nextFangCastTick;
    @Getter
    @Setter
    protected volatile long nextSummonTick;

    public EntityEvokerBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.6, 1.9);
    }

    public boolean isCasting() {
        return casting;
    }

    public void startCasting(int argbColor) {
        this.spellColor = argbColor;
        this.casting = true;
    }

    public void stopCasting() {
        this.casting = false;
    }

    @EventHandler
    protected void onEvokerTick(CEntityTickEvent event) {
        var current = casting;
        if (current == broadcastCasting) {
            return;
        }

        broadcastCasting = current;
        broadcastState();
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        super.writeMetadata(metadata);
        metadata.setFlag(EntityFlag.CASTING, casting);
        metadata.put(EntityDataTypes.EVOKER_SPELL_CASTING_COLOR, casting ? spellColor : 0);
    }
}
