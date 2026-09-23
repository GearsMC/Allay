package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityHeadYawComponent;

/**
 * Demir golem.
 *
 * <p>Tarafsızdır: kendisine vuran oyuncuyu kovalar, yoksa dolaşır. Vanilla'nın köyü koruma
 * ve düşman moblara saldırma davranışı yoktur.</p>
 */
public interface EntityIronGolem extends EntityIntelligent, EntityHeadYawComponent {
}
