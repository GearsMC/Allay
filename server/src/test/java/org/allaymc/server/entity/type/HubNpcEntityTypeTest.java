package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Dekoratif NPC olarak kullanilan moblarin vurulabilir oldugunu dogrular.
 *
 * <p>{@code InventoryTransactionPacketProcessor}, {@code ITEM_USE_ON_ENTITY_ATTACK}
 * isleminde hedef {@link EntityLivingComponent} degilse hicbir sey yapmadan
 * doner. Yani canli varlik bileseni olmayan bir mob'a vurulamaz ve NPC'nin
 * "vurunca komut calistir" mekanigi o turlerde sessizce hic tetiklenmez.
 * Bu test o sessiz kaybi yakalar.</p>
 */
@ExtendWith(AllayTestExtension.class)
class HubNpcEntityTypeTest {

    /** Hub'daki PocketMine NPC'lerinin dayandigi mob turleri. */
    private static Map<String, EntityType<?>> npcMobTypes() {
        var types = new LinkedHashMap<String, EntityType<?>>();
        types.put("allay", EntityTypes.ALLAY);
        types.put("bat", EntityTypes.BAT);
        types.put("frog", EntityTypes.FROG);
        types.put("chicken", EntityTypes.CHICKEN);
        types.put("bee", EntityTypes.BEE);
        types.put("axolotl", EntityTypes.AXOLOTL);
        return types;
    }

    @Test
    void everyNpcMobTypeIsAttackable() {
        Dimension dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();

        npcMobTypes().forEach((name, type) -> {
            var entity = type.createEntity(EntityInitInfo.builder().dimension(dimension).build());
            assertInstanceOf(EntityLivingComponent.class, entity,
                    name + " turu canli varlik degil, oyuncu vuramaz");
        });
    }
}
