package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityFoxBaseComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;

/**
 * Represents a fox with the common animal lifecycle, physics, and AI capabilities.
 */
public interface EntityFox extends EntityAnimal, EntityFoxBaseComponent, EntityHeadYawComponent {

}
