package org.allaymc.server.entity.component;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

/**
 * Bir seyin pesindeyken istemci tarafindaki gorunumu degisen moblar icin temel bilesen.
 *
 * <p>Ofkeli gorunumu bir varlik bayragi surukluyor ve bayraklar istemciye ancak varlik durumu
 * yayinlandiginda ulasiyor. Hedef bulundugunda ya da kaybedildiginde kendiliginden bir yayin
 * yapan hicbir sey yok; bu yuzden bu bilesen hedef hafizasini izleyip degistigi tick'te
 * guncellemeyi kendisi gonderiyor — her tick bir hafiza gozu okumak ucuz, her tick metadata
 * yeniden gondermek degil.</p>
 */
public abstract class EntityAngerableBaseComponentImpl extends EntityBaseComponentImpl {

    @Dependency
    protected EntityAIComponent aiComponent;

    protected boolean angry;

    protected EntityAngerableBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    /**
     * Bir mobun su an bir seyin pesinde olup olmadigini soyler — ya tahrik edilmistir, ya da
     * gorus alanina bir oyuncu girmistir. Metadata yazan tarafla paylasilir ki tel uzerindeki
     * bayrak ile yayini tetikleyen bayrak asla birbirinden ayrilmasin.
     *
     * @param entity incelenecek mob
     * @return mobun ofkeli pozunda gosterilip gosterilmeyecegi
     */
    public static boolean isHunting(EntityIntelligent entity) {
        var memory = entity.getMemoryStorage();
        return memory.get(MemoryTypes.ATTACK_TARGET) != null || memory.get(MemoryTypes.NEAREST_PLAYER) != null;
    }

    /**
     * @return mobun su an saldirmak istedigi bir sey olup olmadigi
     */
    public boolean isAngry() {
        return angry;
    }

    @EventHandler
    protected void onAngerTick(CEntityTickEvent event) {
        var hunting = isHunting((EntityIntelligent) thisEntity);
        if (hunting == angry) {
            return;
        }

        angry = hunting;
        broadcastState();
    }
}
