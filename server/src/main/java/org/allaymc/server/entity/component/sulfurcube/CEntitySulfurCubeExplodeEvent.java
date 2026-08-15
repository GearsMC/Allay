package org.allaymc.server.entity.component.sulfurcube;

import org.allaymc.api.eventbus.event.Event;

/**
 * Bir sulfur kupunun fitili bittiginde tetiklenir.
 *
 * <p>Fitili temel bilesen sayiyor ama patlamayi canli varlik bileseni yapiyor: patlama kupu
 * oldurmek ve bolunmesini engellemek zorunda, ikisi de canin oldugu yerde duruyor.</p>
 */
public class CEntitySulfurCubeExplodeEvent extends Event {

    public static final CEntitySulfurCubeExplodeEvent INSTANCE = new CEntitySulfurCubeExplodeEvent();

    private CEntitySulfurCubeExplodeEvent() {
    }
}
