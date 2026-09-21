package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;

/**
 * Ender Ejderhası.
 *
 * <p>Uçuş fiziği yerçekimsizdir ve vanilla ejderha gibi ne başka varlıklarca itilir ne de
 * bloğun içinden dışarı itilir; yalnızca {@code setMotion} ile verilen hareketi uygular.
 * Işınlanarak yönetilen bir ejderha (hızı sıfır kalan) bundan etkilenmez.</p>
 */
public interface EntityEnderDragon extends EntityLiving, EntityPhysicsComponent, EntityHeadYawComponent {

}
