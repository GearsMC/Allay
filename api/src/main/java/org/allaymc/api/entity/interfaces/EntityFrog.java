package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;

/**
 * Kurbaga.
 *
 * <p>Yapay zekasi yok; canli varlik, fizik ve bas donusu bilesenleri vardir.
 * Yani vurulabilir, hasar alir ve {@code setMotion} ile hareket eder —
 * hareketini yoneten bir davranis grubu eklenene kadar yerinde durur.</p>
 */
public interface EntityFrog extends EntityLiving, EntityPhysicsComponent, EntityHeadYawComponent {
}
