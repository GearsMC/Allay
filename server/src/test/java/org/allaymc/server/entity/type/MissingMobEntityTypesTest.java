package org.allaymc.server.entity.type;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityAnimal;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.entity.component.mob.EntityMobBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AllayTestExtension.class)
class MissingMobEntityTypesTest {

    private static Map<EntityType<?>, Float> intelligentMobHealth() {
        var map = new LinkedHashMap<EntityType<?>, Float>();
        map.put(EntityTypes.HUSK, 20f);
        map.put(EntityTypes.ZOMBIE_VILLAGER, 20f);
        map.put(EntityTypes.ZOMBIE_VILLAGER_V2, 20f);
        map.put(EntityTypes.DROWNED, 20f);
        map.put(EntityTypes.STRAY, 20f);
        map.put(EntityTypes.BOGGED, 16f);
        map.put(EntityTypes.PARCHED, 16f);
        map.put(EntityTypes.CAVE_SPIDER, 12f);
        map.put(EntityTypes.PIGLIN_BRUTE, 50f);
        map.put(EntityTypes.HOGLIN, 40f);
        map.put(EntityTypes.ZOGLIN, 40f);
        map.put(EntityTypes.RAVAGER, 100f);
        map.put(EntityTypes.VEX, 14f);
        map.put(EntityTypes.GHAST, 10f);
        map.put(EntityTypes.BREEZE, 30f);
        map.put(EntityTypes.GUARDIAN, 30f);
        map.put(EntityTypes.ELDER_GUARDIAN, 80f);
        map.put(EntityTypes.SNOW_GOLEM, 4f);
        map.put(EntityTypes.CREAKING, 1f);
        map.put(EntityTypes.WARDEN, 500f);
        map.put(EntityTypes.EVOCATION_ILLAGER, 24f);
        map.put(EntityTypes.MOOSHROOM, 10f);
        map.put(EntityTypes.GOAT, 10f);
        map.put(EntityTypes.OCELOT, 10f);
        map.put(EntityTypes.PANDA, 20f);
        map.put(EntityTypes.PARROT, 6f);
        map.put(EntityTypes.POLAR_BEAR, 30f);
        map.put(EntityTypes.TURTLE, 30f);
        map.put(EntityTypes.ARMADILLO, 12f);
        map.put(EntityTypes.SNIFFER, 14f);
        map.put(EntityTypes.CAMEL, 32f);
        map.put(EntityTypes.CAMEL_HUSK, 32f);
        map.put(EntityTypes.SKELETON_HORSE, 15f);
        map.put(EntityTypes.ZOMBIE_HORSE, 25f);
        map.put(EntityTypes.STRIDER, 20f);
        map.put(EntityTypes.HAPPY_GHAST, 20f);
        map.put(EntityTypes.NAUTILUS, 15f);
        map.put(EntityTypes.ZOMBIE_NAUTILUS, 15f);
        map.put(EntityTypes.SQUID, 10f);
        map.put(EntityTypes.GLOW_SQUID, 10f);
        map.put(EntityTypes.DOLPHIN, 10f);
        map.put(EntityTypes.TADPOLE, 6f);
        map.put(EntityTypes.VILLAGER, 20f);
        map.put(EntityTypes.WANDERING_TRADER, 20f);
        return map;
    }

    @Test
    void everyFormerlyEmptyMobIsLivingIntelligentAndHasVanillaHealth() {
        intelligentMobHealth().forEach((type, health) -> {
            var entity = create(type);
            var id = type.getIdentifier().toString();
            var living = assertInstanceOf(EntityLivingComponent.class, entity, id);
            assertInstanceOf(EntityAIComponent.class, entity, id);
            assertEquals(health, living.getMaxHealth(), id);
            assertEquals(health, living.getHealth(), id);
        });
    }

    @Test
    void randomHealthMobsRollWithinVanillaRange() {
        for (var type : new EntityType<?>[]{EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE,
                EntityTypes.LLAMA, EntityTypes.TRADER_LLAMA}) {
            for (var i = 0; i < 20; i++) {
                var living = assertInstanceOf(EntityLivingComponent.class, create(type));
                var max = living.getMaxHealth();
                assertTrue(max >= 15 && max <= 30, type.getIdentifier() + " -> " + max);
                assertEquals(max, living.getHealth());
            }
        }
    }

    @Test
    void breedableMobsAreAnimals() {
        for (var type : new EntityType<?>[]{EntityTypes.HOGLIN, EntityTypes.MOOSHROOM, EntityTypes.GOAT,
                EntityTypes.OCELOT, EntityTypes.PANDA, EntityTypes.TURTLE, EntityTypes.ARMADILLO, EntityTypes.SNIFFER,
                EntityTypes.CAMEL, EntityTypes.STRIDER, EntityTypes.NAUTILUS}) {
            assertInstanceOf(EntityAnimal.class, create(type), type.getIdentifier().toString());
        }

        var hoglin = (EntityAnimal) create(EntityTypes.HOGLIN);
        assertTrue(hoglin.isBreedingItem(ItemTypes.CRIMSON_FUNGUS.createItemStack()));
        assertFalse(hoglin.isBreedingItem(ItemTypes.WHEAT.createItemStack()));
        var horse = (EntityAnimal) create(EntityTypes.HORSE);
        assertFalse(horse.isBreedingItem(ItemTypes.GOLDEN_CARROT.createItemStack()), "evcil olmayan at ciftlesmez");
    }

    @Test
    void collisionBoxesMatchVanilla() {
        assertBox(EntityTypes.GHAST, 4.0, 4.0);
        assertBox(EntityTypes.RAVAGER, 1.95, 2.2);
        assertBox(EntityTypes.WARDEN, 0.9, 2.9);
        assertBox(EntityTypes.CAVE_SPIDER, 0.7, 0.5);
        assertBox(EntityTypes.ELDER_GUARDIAN, 1.99, 1.99);
        assertBox(EntityTypes.TURTLE, 1.2, 0.4);
        assertBox(EntityTypes.VEX, 0.4, 0.8);
    }

    @Test
    void armedVariantsSpawnWithTheirWeapons() {
        assertHolds(EntityTypes.STRAY, ItemTypes.BOW);
        assertHolds(EntityTypes.BOGGED, ItemTypes.BOW);
        assertHolds(EntityTypes.PARCHED, ItemTypes.BOW);
        assertHolds(EntityTypes.PIGLIN_BRUTE, ItemTypes.GOLDEN_AXE);
        assertHolds(EntityTypes.VEX, ItemTypes.IRON_SWORD);
    }

    @Test
    void variantsAreRolledAndKeptAcrossSaves() {
        var horse = create(EntityTypes.HORSE);
        var base = (EntityMobBaseComponentImpl) ((EntityImpl) horse).getBaseComponent();
        var variant = base.getVariant();
        var markVariant = base.getMarkVariant();
        assertTrue(variant >= 0 && variant <= 6);
        assertTrue(markVariant >= 0 && markVariant <= 4);

        var reloaded = EntityTypes.HORSE.createEntity(EntityInitInfo.builder().nbt(horse.saveNBT()).build());
        var reloadedBase = (EntityMobBaseComponentImpl) ((EntityImpl) reloaded).getBaseComponent();
        assertEquals(variant, reloadedBase.getVariant());
        assertEquals(markVariant, reloadedBase.getMarkVariant());
    }

    @Test
    void propertyDrivenMobsRegisterTheirProperties() {
        assertTrue(EntityTypes.CREAKING.getProperties().containsKey(EntityPropertyTypes.CREAKING_STATE.getName()));
        assertTrue(EntityTypes.CREAKING.getProperties().containsKey(EntityPropertyTypes.CREAKING_SWAYING_TICKS.getName()));
        assertTrue(EntityTypes.ARMADILLO.getProperties().containsKey(EntityPropertyTypes.ARMADILLO_STATE.getName()));
    }

    @Test
    void auxiliaryEntitiesAreRegistered() {
        assertNotNull(create(EntityTypes.EVOCATION_FANG));
        assertNotNull(create(EntityTypes.LLAMA_SPIT));
        var npc = assertInstanceOf(EntityLivingComponent.class, create(EntityTypes.NPC));
        assertFalse(npc.canBeAttacked(DamageContainer.fall(5)), "NPC oyun hasari almaz");
        assertTrue(npc.canBeAttacked(DamageContainer.simpleAttack(5)), "eklenti API hasari NPC'yi hala oldurebilir");
    }

    private static Entity create(EntityType<?> type) {
        return type.createEntity(EntityInitInfo.builder().build());
    }

    private static void assertBox(EntityType<?> type, double width, double height) {
        var aabb = create(type).getBaseAABB();
        assertEquals(width, aabb.maxX() - aabb.minX(), 1e-6, type.getIdentifier().toString());
        assertEquals(height, aabb.maxY() - aabb.minY(), 1e-6, type.getIdentifier().toString());
    }

    private static void assertHolds(EntityType<?> type, ItemType<?> weapon) {
        var holder = assertInstanceOf(EntityContainerHolderComponent.class, create(type));
        assertEquals(weapon, holder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType(),
                type.getIdentifier().toString());
    }
}
