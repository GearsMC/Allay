package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;

/**
 * Zombi domuz adam (zombified piglin).
 *
 * <p>Altın kılıçla doğar ve tarafsızdır: yalnızca kendisine vuran oyuncuyu kovalar. Vanilla'daki
 * sürü halinde öfkelenme yoktur.</p>
 */
public interface EntityZombiePigman extends EntityIntelligent, EntityHeadYawComponent, EntityUndeadComponent,
        EntityContainerHolderComponent, EntityWeaponStanceComponent {
}
