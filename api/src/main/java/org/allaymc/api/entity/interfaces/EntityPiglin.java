package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;

public interface EntityPiglin extends EntityIntelligent, EntityHeadYawComponent, EntityContainerHolderComponent, EntityBabyComponent, EntityWeaponStanceComponent {
}
