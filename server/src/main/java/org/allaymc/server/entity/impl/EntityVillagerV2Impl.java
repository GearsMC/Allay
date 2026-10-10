package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityVillagerV2;
import org.allaymc.server.component.ComponentProvider;
import org.allaymc.server.entity.component.mob.EntityMobBaseComponentImpl;

import java.util.List;

public class EntityVillagerV2Impl extends EntityImpl implements EntityVillagerV2 {

    @Delegate
    private EntityLivingComponent livingComponent;

    @Delegate
    private EntityPhysicsComponent physicsComponent;

    @Delegate
    private EntityAIComponent aiComponent;

    @Delegate
    private EntityParallelTickComponent parallelTickComponent;

    @Delegate
    private EntityHeadYawComponent headYawComponent;

    public EntityVillagerV2Impl(EntityInitInfo initInfo,
                                List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }

    @Override
    public int getProfession() {
        // The client reads profession from VARIANT. Keep that value on the mob component so a
        // cured villager reloads with the same profession the zombie villager had.
        return getBaseComponent() instanceof EntityMobBaseComponentImpl mob ? mob.getVariant() : 0;
    }

    @Override
    public void setProfession(int profession) {
        if (getBaseComponent() instanceof EntityMobBaseComponentImpl mob) {
            mob.setVariant(profession);
            return;
        }
        broadcastState();
    }
}
