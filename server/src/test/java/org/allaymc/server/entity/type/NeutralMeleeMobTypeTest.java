package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demir golem ve zombi domuz adamın canlı, yapay zekâlı mob olduğunu ve vanilla ganimetini
 * doğrular. İkisi de önceden yalnızca taban bileşenle kayıtlıydı: vurulamıyor, düşmüyor,
 * ölünce hiçbir şey bırakmıyordu (GearsCore ada spawner'ı bu türleri doğuruyor).
 */
@ExtendWith(AllayTestExtension.class)
class NeutralMeleeMobTypeTest {

    @Test
    void ikiTurDeCanliVeYapayZekali() {
        Dimension dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        var golem = EntityTypes.IRON_GOLEM.createEntity(EntityInitInfo.builder().dimension(dimension).build());
        var pigman = EntityTypes.ZOMBIE_PIGMAN.createEntity(EntityInitInfo.builder().dimension(dimension).build());
        assertInstanceOf(EntityIntelligent.class, golem);
        assertInstanceOf(EntityIntelligent.class, pigman);
        assertEquals(100f, ((EntityLivingComponent) golem).getMaxHealth());
        assertEquals(20f, ((EntityLivingComponent) pigman).getMaxHealth());
        assertTrue(((EntityLivingComponent) pigman).isFireproof());
    }

    @Test
    void golemDemirVeGelincikDusurur() {
        Dimension dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        var golem = (EntityLivingComponent) EntityTypes.IRON_GOLEM.createEntity(
                EntityInitInfo.builder().dimension(dimension).build());
        for (int i = 0; i < 50; i++) {
            int iron = 0;
            for (ItemStack drop : golem.getDrops(0)) {
                if (drop.getItemType() == ItemTypes.IRON_INGOT) {
                    iron += drop.getCount();
                } else {
                    assertEquals(ItemTypes.POPPY, drop.getItemType());
                    assertTrue(drop.getCount() >= 1 && drop.getCount() <= 2);
                }
            }
            assertTrue(iron >= 3 && iron <= 5, "demir: " + iron);
        }
        assertEquals(0, golem.getDropXpAmount());
    }
}
