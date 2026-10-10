package org.allaymc.server.network.protocol;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.server.network.protocol.v766.PacketEncoder_v766;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(AllayTestExtension.class)
class NewMobMetadataEncoderTest {

    private final PacketEncoder encoder = new PacketEncoder_v766(mock(ProtocolData.class));

    private static List<EntityType<?>> newTypes() {
        return List.of(EntityTypes.HUSK, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIE_VILLAGER_V2, EntityTypes.DROWNED,
                EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.PARCHED, EntityTypes.CAVE_SPIDER, EntityTypes.PIGLIN_BRUTE,
                EntityTypes.HOGLIN, EntityTypes.ZOGLIN, EntityTypes.RAVAGER, EntityTypes.VEX, EntityTypes.GHAST,
                EntityTypes.BREEZE, EntityTypes.GUARDIAN, EntityTypes.ELDER_GUARDIAN, EntityTypes.SNOW_GOLEM,
                EntityTypes.CREAKING, EntityTypes.WARDEN, EntityTypes.EVOCATION_ILLAGER, EntityTypes.EVOCATION_FANG,
                EntityTypes.MOOSHROOM, EntityTypes.GOAT, EntityTypes.OCELOT, EntityTypes.PANDA, EntityTypes.PARROT,
                EntityTypes.POLAR_BEAR, EntityTypes.TURTLE, EntityTypes.ARMADILLO, EntityTypes.SNIFFER, EntityTypes.CAMEL,
                EntityTypes.CAMEL_HUSK, EntityTypes.LLAMA, EntityTypes.TRADER_LLAMA, EntityTypes.LLAMA_SPIT,
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE,
                EntityTypes.STRIDER, EntityTypes.HAPPY_GHAST, EntityTypes.NAUTILUS, EntityTypes.ZOMBIE_NAUTILUS,
                EntityTypes.SQUID, EntityTypes.GLOW_SQUID, EntityTypes.DOLPHIN, EntityTypes.TADPOLE, EntityTypes.VILLAGER,
                EntityTypes.WANDERING_TRADER, EntityTypes.NPC);
    }

    private static Entity create(EntityType<?> type) {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        return type.createEntity(EntityInitInfo.builder()
                .dimension(spawn.dimension())
                .pos(spawn.x() + 0.5, spawn.y() + 80, spawn.z() + 0.5)
                .build());
    }

    @Test
    void everyNewMobEncodesMetadata() {
        for (var type : newTypes()) {
            var entity = create(type);
            var metadata = assertDoesNotThrow(() -> encoder.encodeEntityMetadata(entity, null), type.getIdentifier().toString());
            assertNotNull(metadata.get(EntityDataTypes.SCALE), type.getIdentifier().toString());
        }
    }

    @Test
    void variantMobsSendTheirVariant() {
        var horse = create(EntityTypes.HORSE);
        var metadata = encoder.encodeEntityMetadata(horse, null);
        assertNotNull(metadata.get(EntityDataTypes.VARIANT));
        assertNotNull(metadata.get(EntityDataTypes.MARK_VARIANT));

        var traderLlama = create(EntityTypes.TRADER_LLAMA);
        assertEquals(1, (int) encoder.encodeEntityMetadata(traderLlama, null).get(EntityDataTypes.MARK_VARIANT));
    }

    @Test
    void elderGuardianAndGhastCarryTheirFlags() {
        var elder = create(EntityTypes.ELDER_GUARDIAN);
        assertTrue(Boolean.TRUE.equals(encoder.encodeEntityMetadata(elder, null).getFlags().get(EntityFlag.ELDER)));

        var ghast = create(EntityTypes.GHAST);
        assertTrue(Boolean.TRUE.equals(encoder.encodeEntityMetadata(ghast, null).getFlags().get(EntityFlag.FIRE_IMMUNE)));
    }
}
