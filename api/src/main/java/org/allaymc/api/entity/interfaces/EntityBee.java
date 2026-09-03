package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;

/**
 * Ari.
 *
 * <p>Yapay zekasi henuz yok; canli varlik, fizik ve bas donusu bilesenleri
 * vardir. Yani vurulabilir, hasar alir ve {@code setMotion} ile hareket eder —
 * ucusunu yoneten bir davranis grubu eklenene kadar yerinde asili durur.</p>
 */
public interface EntityBee extends EntityLiving, EntityPhysicsComponent, EntityHeadYawComponent {
}
