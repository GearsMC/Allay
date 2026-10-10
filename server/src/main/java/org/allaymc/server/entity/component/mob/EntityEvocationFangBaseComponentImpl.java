package org.allaymc.server.entity.component.mob;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.action.SimpleEntityAction;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.interfaces.EntityEvocationIllager;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPillager;
import org.allaymc.api.entity.interfaces.EntityVindicator;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.world.WorldViewer;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.component.EntityMetadataContributor;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

public class EntityEvocationFangBaseComponentImpl extends EntityBaseComponentImpl implements EntityMetadataContributor {

    public static final int LIFETIME_TICKS = 22;
    protected static final int BITE_TICK = 8;
    protected static final float DAMAGE = 6;

    @Getter
    @Setter
    protected int warmupTicks;
    @Getter
    @Setter
    protected long ownerRuntimeId = -1L;

    protected int age;
    protected boolean bitten;

    public EntityEvocationFangBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.25, 0.0, -0.25, 0.25, 0.8, 0.25);
    }

    @Override
    public void spawnTo(WorldViewer viewer) {
        var alreadyViewing = getViewers().contains(viewer);
        super.spawnTo(viewer);
        if (!alreadyViewing && age > warmupTicks) {
            viewer.viewEntityAction(thisEntity, SimpleEntityAction.SWING_ARM);
        }
    }

    @EventHandler
    protected void onFangTick(CEntityTickEvent event) {
        if (thisEntity.willBeDespawnedLater()) {
            return;
        }

        if (age == warmupTicks) {
            applyAction(SimpleEntityAction.SWING_ARM);
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_EVOCATION_FANGS_ATTACK));
        }
        if (!bitten && age >= warmupTicks + BITE_TICK) {
            bitten = true;
            bite();
        }
        if (age >= warmupTicks + LIFETIME_TICKS) {
            thisEntity.remove();
        }
        age++;
    }

    protected void bite() {
        var owner = ownerRuntimeId == -1L ? null : getDimension().getEntityManager().getEntity(ownerRuntimeId);
        var box = new AABBd(getOffsetAABB()).expand(0.1);
        for (var entity : getDimension().getEntityManager().getPhysicsService().computeCollidingEntities(box, e -> e instanceof EntityLiving)) {
            if (entity == owner || isIllager(entity) || !entity.isAlive()) {
                continue;
            }

            var damage = owner == null
                    ? DamageContainer.magicEffect(DAMAGE)
                    : new DamageContainer(owner, DamageType.MAGIC, DAMAGE);
            ((EntityLiving) entity).attack(damage);
        }
    }

    protected static boolean isIllager(Object entity) {
        return entity instanceof EntityEvocationIllager || entity instanceof EntityVindicator || entity instanceof EntityPillager;
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        metadata.put(EntityDataTypes.DATA_LIFETIME_TICKS, Math.max(0, warmupTicks + LIFETIME_TICKS - age));
    }
}
