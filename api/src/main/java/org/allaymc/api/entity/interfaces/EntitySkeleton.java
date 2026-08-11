package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;

public interface EntitySkeleton extends EntityIntelligent, EntityHeadYawComponent, EntityUndeadComponent, EntityContainerHolderComponent, EntityWeaponStanceComponent {

}
