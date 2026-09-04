package org.allaymc.server.utils;

import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.type.BlockEntityTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.enchantment.EnchantmentTypes;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.NBTIO;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import org.cloudburstmc.nbt.NbtMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author daoge_cmd
 */
@ExtendWith(AllayTestExtension.class)
class AllayNBTIOTest {
    @Test
    void testFromBlockStateNBT() {
        var block1 = BlockTypes.BAMBOO.getDefaultState();
        var nbt = block1.getBlockStateNBT();
        var block2 = NBTIO.getAPI().fromBlockStateNBT(nbt);
        assertEquals(block1, block2);
    }

    @Test
    void testFromItemStackNBT() {
        var item1 = ItemTypes.DIAMOND.createItemStack(1, 1);
        item1.addEnchantment(EnchantmentTypes.SHARPNESS, 1);
        item1.setLore(List.of("test"));
        item1.setCustomName("test");
        var nbt = item1.saveNBT();
        var item2 = NBTIO.getAPI().fromItemStackNBT(nbt);
        assertTrue(item1.canMerge(item2));
    }

    @Test
    void testFromEntityNBT() {
        var entity1 = EntityTypes.VILLAGER_V2.createEntity(
                EntityInitInfo
                        .builder()
                        .loc(Server.getInstance().getWorldPool().getGlobalSpawnPoint())
                        .build()
        );
        var nbt1 = entity1.saveNBT();
        var entity2 = NBTIO.getAPI().fromEntityNBT(Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension(), nbt1);
        var nbt2 = entity2.saveNBT();
        assertEquals(nbt1, nbt2);
    }

    @Test
    void testFromBlockEntityNBT() {
        var blockEntity1 = BlockEntityTypes.CHEST.createBlockEntity(
                BlockEntityInitInfo
                        .builder()
                        .dimension(Server.getInstance().getWorldPool().getDefaultWorld().getOverWorld())
                        .pos(0, 0, 0)
                        .build()
        );
        var nbt1 = blockEntity1.saveNBT();
        var blockEntity2 = NBTIO.getAPI().fromBlockEntityNBT(Server.getInstance().getWorldPool().getDefaultWorld().getOverWorld(), nbt1);
        var nbt2 = blockEntity2.saveNBT();
        assertEquals(nbt1, nbt2);
    }

    /**
     * PocketMine eklentileri varlik kimligine PHP sinif adi yazabiliyor
     * (ornegin {@code brokiem\snpc\entity\CustomHuman}). Ters bolu
     * {@link org.allaymc.api.utils.identifier.Identifier} icin gecersiz oldugundan
     * kurucu istisna atiyordu; istisna cagirana kadar cikip o chunk'taki BUTUN
     * varliklarin okunmasini iptal ediyordu. Bicimsiz kimlik, bilinmeyen tip gibi
     * {@code null} donmeli ki cagiran yalnizca o varligi atlasin.
     */
    @Test
    void testFromEntityNBTReturnsNullForMalformedIdentifier() {
        var nbt = NbtMap.builder()
                .putString("identifier", "brokiem\\snpc\\entity\\CustomHuman")
                .build();

        var dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        assertNull(NBTIO.getAPI().fromEntityNBT(dimension, nbt));
    }
}
