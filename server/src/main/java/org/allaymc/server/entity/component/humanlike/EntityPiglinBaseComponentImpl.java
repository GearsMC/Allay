package org.allaymc.server.entity.component.humanlike;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.item.type.ItemTypes;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Piglin'in temel davranisi: carpisma kutusu, silah varyanti ve savas animasyonu durumu.
 *
 * <p>Piglin ya arbaletle ya da altin kilicla dogar, yari yariya. Bu secim davranis grubunu da
 * belirler — arbaletli varyant mesafeden ates eder, kiliclisi ustune kosar; ikisi de calisma
 * aninda el gozune bakilarak secilir.</p>
 *
 * <p>Geri kalan her sey — yalnizca bos eli silahlandirip kayitli bir silahin yeniden baslatmayi
 * atlatmasini saglamak ve istemcinin arbaleti canlandirmak icin ihtiyac duydugu durumu tasimak —
 * diger silahli moblarin da kullandigi {@link EntityArmedBaseComponentImpl}'den geliyor.</p>
 */
public class EntityPiglinBaseComponentImpl extends EntityArmedBaseComponentImpl {

    public EntityPiglinBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, () -> ThreadLocalRandom.current().nextBoolean()
                ? ItemTypes.CROSSBOW
                : ItemTypes.GOLDEN_SWORD, 0.6, 1.95);
    }
}
