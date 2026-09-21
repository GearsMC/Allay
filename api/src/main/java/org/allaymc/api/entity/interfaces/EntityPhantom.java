package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityUndeadComponent;

/**
 * Fantom.
 *
 * <p>Yapay zekası henüz yok; uçuş fiziği (yerçekimi yok) ve baş dönüşü vardır, yani
 * {@code setMotion} ile havada hareket ettirilebilir.</p>
 */
public interface EntityPhantom extends Entity, EntityUndeadComponent, EntityPhysicsComponent, EntityHeadYawComponent {

}
