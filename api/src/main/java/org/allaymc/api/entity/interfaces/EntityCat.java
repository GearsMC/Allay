package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;

/**
 * Kedi.
 *
 * <p>Yapay zekası henüz yok; canlı varlık, fizik, baş dönüşü ve yavru bileşenleri vardır.
 * Yani yerçekimine uyar ve {@code setMotion} ile yürütülebilir, ama kendi başına dolaşmaz.</p>
 */
public interface EntityCat extends EntityLiving, EntityPhysicsComponent, EntityHeadYawComponent, EntityBabyComponent {

}
