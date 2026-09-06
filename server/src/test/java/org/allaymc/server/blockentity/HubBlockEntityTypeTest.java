package org.allaymc.server.blockentity;

import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.type.BlockEntityType;
import org.allaymc.api.blockentity.type.BlockEntityTypes;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PocketMine hub dunyalarinda bulunan ama motorda karsiligi olmayan vanilla
 * blok varliklarini dogrular.
 *
 * <p>Kayitli olmayan bir blok varligi {@code AllayNBTIO.fromBlockEntityNBT}
 * icinde uyariyla atlanir; blok yerinde kalir ama verisi <b>chunk bir sonraki
 * kayitta yeniden yazilirken kalici olarak dusrer</b>. Hub'da 34 blok bu
 * durumdaydi: 18 potent sulfur, 14 gun isigi sensoru, bir crafter ve bir mob
 * doguran.</p>
 */
@ExtendWith(AllayTestExtension.class)
class HubBlockEntityTypeTest {

    private static Map<String, BlockEntityType<?>> hubBlockEntityTypes() {
        var types = new LinkedHashMap<String, BlockEntityType<?>>();
        types.put("DaylightDetector", BlockEntityTypes.DAYLIGHT_DETECTOR);
        types.put("PotentSulfurBlock", BlockEntityTypes.POTENT_SULFUR);
        types.put("Crafter", BlockEntityTypes.CRAFTER);
        types.put("MobSpawner", BlockEntityTypes.MOB_SPAWNER);
        return types;
    }

    @Test
    void everyHubBlockEntityTypeIsRegistered() {
        hubBlockEntityTypes().forEach((id, type) -> {
            assertNotNull(type, id + " turu kayitli degil");
            assertNotNull(Registries.BLOCK_ENTITIES.get(id),
                    id + " kayit defterinde yok, chunk yuklenirken atlanir");
        });
    }

    @Test
    void markerBlockEntitiesSurviveASaveLoadCycle() {
        // PocketMine hub'inda bu ikisi id/x/y/z disinda hicbir veri tasimiyor.
        for (String id : new String[]{"DaylightDetector", "PotentSulfurBlock"}) {
            var blockEntity = create(Registries.BLOCK_ENTITIES.get(id));
            NbtMap saved = blockEntity.saveNBT();

            assertEquals(id, saved.getString("id"));
            assertEquals(1, saved.getInt("x"));
            assertEquals(2, saved.getInt("y"));
            assertEquals(3, saved.getInt("z"));
        }
    }

    @Test
    void crafterKeepsItsDisabledSlots() {
        var crafter = create(BlockEntityTypes.CRAFTER);

        // Hub'daki crafter'in gercek degeri.
        crafter.loadNBT(crafter.saveNBT().toBuilder().putShort("disabled_slots", (short) 295).build());

        assertEquals((short) 295, crafter.saveNBT().getShort("disabled_slots"),
                "devre disi slot maskesi kaydedilip geri yuklenmeli");

        // Int olarak yazilmis eski kayitlar da okunabilmeli.
        crafter.loadNBT(crafter.saveNBT().toBuilder().putInt("disabled_slots", 73).build());

        assertEquals((short) 73, crafter.saveNBT().getShort("disabled_slots"),
                "int olarak kaydedilmis maske de geri yuklenmeli");
    }

    @Test
    void mobSpawnerKeepsItsSpawnSettings() {
        var spawner = create(BlockEntityTypes.MOB_SPAWNER);

        // Hub'daki doguranin gercek degerleri.
        spawner.loadNBT(spawner.saveNBT().toBuilder()
                .putString("EntityIdentifier", "minecraft:zombie")
                .putShort("Delay", (short) 200)
                .putShort("MinSpawnDelay", (short) 200)
                .putShort("MaxSpawnDelay", (short) 800)
                .putShort("SpawnCount", (short) 1)
                .putShort("MaxNearbyEntities", (short) 6)
                .putShort("RequiredPlayerRange", (short) 16)
                .putShort("SpawnRange", (short) 4)
                .build());

        NbtMap saved = spawner.saveNBT();
        assertEquals("minecraft:zombie", saved.getString("EntityIdentifier"));
        assertEquals(200, saved.getShort("MinSpawnDelay"));
        assertEquals(800, saved.getShort("MaxSpawnDelay"));
        assertEquals(6, saved.getShort("MaxNearbyEntities"));
        assertEquals(16, saved.getShort("RequiredPlayerRange"));
        assertEquals(4, saved.getShort("SpawnRange"));
    }

    private static org.allaymc.api.blockentity.BlockEntity create(BlockEntityType<?> type) {
        Dimension dimension = Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
        return type.createBlockEntity(BlockEntityInitInfo.builder()
                .dimension(dimension)
                .pos(1, 2, 3)
                .build());
    }
}
