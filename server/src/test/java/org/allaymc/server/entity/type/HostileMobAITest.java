package org.allaymc.server.entity.type;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityCreeperBaseComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.data.WeaponStance;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wolf, enderman, endermite, piglin and blaze against silently falling back to the component
 * -less stubs in {@code EntityTypeDefaultInitializer}. Registration there is guarded by a
 * {@code == null} check, so a rename or a removed {@code initXxx()} would not fail the build — the
 * mobs would just quietly go back to standing still.
 */
@ExtendWith(AllayTestExtension.class)
public class HostileMobAITest {

    @Test
    void hostileMobsHaveBehaviors() {
        assertHasBehaviors(EntityTypes.WOLF);
        assertHasBehaviors(EntityTypes.ENDERMAN);
        assertHasBehaviors(EntityTypes.ENDERMITE);
        assertHasBehaviors(EntityTypes.PIGLIN);
        assertHasBehaviors(EntityTypes.BLAZE);
        assertHasBehaviors(EntityTypes.CREEPER);
        assertHasBehaviors(EntityTypes.WITCH);
        assertHasBehaviors(EntityTypes.SKELETON);
        assertHasBehaviors(EntityTypes.PILLAGER);
        assertHasBehaviors(EntityTypes.VINDICATOR);
    }

    @Test
    void hostileMobsHaveExpectedHealth() {
        assertEquals(8, maxHealthOf(EntityTypes.WOLF));
        assertEquals(40, maxHealthOf(EntityTypes.ENDERMAN));
        assertEquals(8, maxHealthOf(EntityTypes.ENDERMITE));
        assertEquals(16, maxHealthOf(EntityTypes.PIGLIN));
        assertEquals(20, maxHealthOf(EntityTypes.BLAZE));
        assertEquals(20, maxHealthOf(EntityTypes.CREEPER));
        assertEquals(26, maxHealthOf(EntityTypes.WITCH));
        assertEquals(20, maxHealthOf(EntityTypes.SKELETON));
        assertEquals(24, maxHealthOf(EntityTypes.PILLAGER));
        assertEquals(24, maxHealthOf(EntityTypes.VINDICATOR));
    }

    @Test
    void armedMobsSpawnHoldingTheirWeapon() {
        assertHoldsWeapon(EntityTypes.SKELETON, ItemTypes.BOW);
        assertHoldsWeapon(EntityTypes.PILLAGER, ItemTypes.CROSSBOW);
        assertHoldsWeapon(EntityTypes.VINDICATOR, ItemTypes.IRON_AXE);
    }

    /**
     * The client animates bows, crossbows and raised illager weapons from entity flags, which are
     * only written for mobs carrying this component. Losing it compiles fine and leaves the mobs
     * fighting with their weapons lowered, so it is pinned down here.
     */
    @Test
    void armedMobsCarryWeaponStance() {
        assertStartsIdle(EntityTypes.SKELETON);
        assertStartsIdle(EntityTypes.PILLAGER);
        assertStartsIdle(EntityTypes.VINDICATOR);
        assertStartsIdle(EntityTypes.PIGLIN);
    }

    @Test
    void creeperStartsWithAnUnlitFuse() {
        var creeper = assertInstanceOf(EntityCreeperBaseComponent.class,
                EntityTypes.CREEPER.createEntity(EntityInitInfo.builder().build()));
        assertFalse(creeper.isSwelling(), "a freshly spawned creeper must not already be swelling");
    }

    @Test
    void blazeFliesAndIgnoresFire() {
        var blaze = EntityTypes.BLAZE.createEntity(EntityInitInfo.builder().build());

        var living = assertInstanceOf(EntityLivingComponent.class, blaze);
        assertTrue(living.isFireproof(), "a blaze must not be hurt by its own element");

        var physics = assertInstanceOf(EntityPhysicsComponent.class, blaze);
        assertEquals(0, physics.getGravity(), "a blaze hovers, so gravity must not pull it down");
    }

    @Test
    void piglinSpawnsWithACrossbowOrAGoldenSword() {
        var seen = new HashSet<Object>();
        // The weapon is a coin flip per spawn; over this many spawns seeing only one of the two
        // would mean the roll is broken rather than unlucky.
        for (int i = 0; i < 100; i++) {
            var piglin = EntityTypes.PIGLIN.createEntity(EntityInitInfo.builder().build());
            var containerHolder = assertInstanceOf(EntityContainerHolderComponent.class, piglin);
            var weapon = containerHolder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType();
            assertTrue(weapon == ItemTypes.CROSSBOW || weapon == ItemTypes.GOLDEN_SWORD,
                    "piglin spawned holding " + weapon.getIdentifier());
            seen.add(weapon);
        }

        assertEquals(2, seen.size(), "piglins only ever rolled one weapon variant");
    }

    private static void assertHasBehaviors(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var aiComponent = assertInstanceOf(EntityAIComponent.class, entity,
                entityType.getIdentifier() + " has no AI component");
        assertFalse(aiComponent.getBehaviorGroup().getBehaviors().isEmpty(),
                entityType.getIdentifier() + " has an empty behavior group");
    }

    private static void assertStartsIdle(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var armed = assertInstanceOf(EntityWeaponStanceComponent.class, entity,
                entityType.getIdentifier() + " cannot report a weapon stance, so its weapon will never animate");
        assertEquals(WeaponStance.IDLE, armed.getWeaponStance(),
                entityType.getIdentifier() + " starts out already drawing its weapon");
        assertFalse(armed.isAggressive(),
                entityType.getIdentifier() + " starts out already aggressive");
    }

    private static void assertHoldsWeapon(EntityType<?> entityType, ItemType<?> expected) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var containerHolder = assertInstanceOf(EntityContainerHolderComponent.class, entity,
                entityType.getIdentifier() + " cannot hold anything");
        assertEquals(expected, containerHolder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType(),
                entityType.getIdentifier() + " spawned with the wrong weapon");
    }

    private static float maxHealthOf(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        return assertInstanceOf(EntityLivingComponent.class, entity).getMaxHealth();
    }
}
