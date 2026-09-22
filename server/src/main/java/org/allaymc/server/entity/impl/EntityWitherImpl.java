package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityWither;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityWitherImpl extends EntityImpl implements EntityWither {
    @Delegate
    private EntityLivingComponent livingComponent;
    @Delegate
    private EntityPhysicsComponent physicsComponent;
    @Delegate
    private EntityHeadYawComponent headYawComponent;
    private int witherInvulnerableTicks;
    private long witherTargetA;
    private long witherTargetB;
    private long witherTargetC;

    public EntityWitherImpl(EntityInitInfo initInfo,
                            List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }

    @Override
    public int getWitherInvulnerableTicks() {
        return witherInvulnerableTicks;
    }

    @Override
    public void setWitherInvulnerableTicks(int ticks) {
        witherInvulnerableTicks = Math.max(0, ticks);
        syncWitherMetadata();
    }

    @Override
    public long getWitherTargetA() {
        return witherTargetA;
    }

    @Override
    public long getWitherTargetB() {
        return witherTargetB;
    }

    @Override
    public long getWitherTargetC() {
        return witherTargetC;
    }

    @Override
    public void setWitherTargets(long first, long second, long third) {
        if (witherTargetA == first && witherTargetB == second && witherTargetC == third) return;
        witherTargetA = first;
        witherTargetB = second;
        witherTargetC = third;
        syncWitherMetadata();
    }

    private void syncWitherMetadata() {
        if (isSpawned()) forEachViewers(viewer -> viewer.viewEntityState(this));
    }
}
