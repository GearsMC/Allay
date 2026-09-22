package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;

public interface EntityWitherSkeleton extends EntityLiving, EntityPhysicsComponent, EntityHeadYawComponent,
        EntityUndeadComponent, EntityContainerHolderComponent, EntityWeaponStanceComponent {

}
