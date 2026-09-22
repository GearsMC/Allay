package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;

public interface EntityWither extends EntityLiving, EntityPhysicsComponent, EntityHeadYawComponent, EntityUndeadComponent {

    int getWitherInvulnerableTicks();

    void setWitherInvulnerableTicks(int ticks);

    long getWitherTargetA();

    long getWitherTargetB();

    long getWitherTargetC();

    void setWitherTargets(long first, long second, long third);

}
