package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.concurrent.ThreadLocalRandom;

public class EntityVexBaseComponentImpl extends EntityMobBaseComponentImpl {

    public static final MemoryType<Long> OWNER = new MemoryType<>(new Identifier("gearsmc:vex_owner"));

    protected static final String TAG_LIFE_TICKS = "LifeTicks";
    protected static final int MIN_LIFE_SECONDS = 30;
    protected static final int MAX_LIFE_SECONDS = 119;

    @Dependency
    protected EntityLivingComponent livingComponent;
    @Dependency
    protected EntityAIComponent aiComponent;

    protected int lifeTicks = -1;

    public EntityVexBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.4, 0.8);
        huntingFlag(EntityFlag.CHARGING);
    }

    public void bindOwner(Entity owner) {
        aiComponent.getMemoryStorage().put(OWNER, owner.getRuntimeId());
        lifeTicks = 20 * ThreadLocalRandom.current().nextInt(MIN_LIFE_SECONDS, MAX_LIFE_SECONDS + 1);
    }

    @EventHandler
    protected void onVexTick(CEntityTickEvent event) {
        if (lifeTicks < 0 || !thisEntity.isAlive()) {
            return;
        }

        if (lifeTicks > 0) {
            lifeTicks--;
        } else if (event.getCurrentTick() % 20 == 0) {
            livingComponent.attack(DamageContainer.magicEffect(1), true);
        }
    }

    @EventHandler
    protected void onVexLoadNBT(CEntityLoadNBTEvent event) {
        event.getNbt().listenForInt(TAG_LIFE_TICKS, value -> lifeTicks = value);
    }

    @EventHandler
    protected void onVexSaveNBT(CEntitySaveNBTEvent event) {
        event.getNbt().putInt(TAG_LIFE_TICKS, lifeTicks);
    }
}
