package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityAnimalComponent;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityAnimal;
import org.allaymc.api.entity.interfaces.EntityFox;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.allaymc.server.entity.ai.behavior.BehaviorImpl;
import org.allaymc.server.entity.ai.executor.EntityBreedingExecutor;
import org.allaymc.server.entity.ai.executor.FlatRandomRoamExecutor;
import org.allaymc.server.entity.ai.executor.FollowEntityExecutor;
import org.allaymc.server.entity.ai.executor.InLoveExecutor;
import org.allaymc.server.entity.ai.executor.LookAtEntityExecutor;
import org.allaymc.server.entity.ai.route.finder.FlatAStarRouteFinder;
import org.allaymc.server.entity.ai.sensor.NearestFeedingPlayerSensor;
import org.allaymc.server.entity.ai.sensor.NearestPlayerSensor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prevents the fox registration from falling back to the visual-only default entity type.
 */
@ExtendWith(AllayTestExtension.class)
class FoxEntityTypeTest {

    @Test
    void foxExposesEveryAnimalCapability() {
        var fox = createFox();

        assertInstanceOf(EntityAnimal.class, fox);
        assertInstanceOf(EntityIntelligent.class, fox);
        assertInstanceOf(EntityLivingComponent.class, fox);
        assertInstanceOf(EntityPhysicsComponent.class, fox);
        assertInstanceOf(EntityAIComponent.class, fox);
        assertInstanceOf(EntityParallelTickComponent.class, fox);
        assertInstanceOf(EntityAnimalComponent.class, fox);
        assertInstanceOf(EntityBabyComponent.class, fox);
        assertInstanceOf(EntityHeadYawComponent.class, fox);
    }

    @Test
    void foxHasVanillaHealthAndBreedingItems() {
        var fox = createFox();
        var living = assertInstanceOf(EntityLivingComponent.class, fox);
        var animal = assertInstanceOf(EntityAnimalComponent.class, fox);

        assertEquals(10, living.getMaxHealth());
        assertEquals(10, living.getHealth());
        assertTrue(animal.isBreedingItem(ItemTypes.SWEET_BERRIES.createItemStack()));
        assertTrue(animal.isBreedingItem(ItemTypes.GLOW_BERRIES.createItemStack()));
        assertFalse(animal.isBreedingItem(ItemTypes.WHEAT.createItemStack()));
    }

    @Test
    void foxHasAReadyNavigationPipeline() {
        var ai = assertInstanceOf(EntityAIComponent.class, createFox());

        assertNotNull(ai.getBehaviorGroup());
        assertInstanceOf(FlatAStarRouteFinder.class, ai.getBehaviorGroup().getRouteFinder());
        assertFalse(ai.getBehaviorGroup().getControllers().isEmpty());
    }

    @Test
    void foxHasItsBasicBehaviorSet() {
        var ai = assertInstanceOf(EntityAIComponent.class, createFox());
        var group = ai.getBehaviorGroup();

        assertTrue(group.getSensors().stream().anyMatch(NearestFeedingPlayerSensor.class::isInstance));
        assertTrue(group.getSensors().stream().anyMatch(NearestPlayerSensor.class::isInstance));
        assertEquals(1, group.getCoreBehaviors().size());
        assertTrue(executors(group.getCoreBehaviors()).contains(InLoveExecutor.class));

        var executors = executors(group.getBehaviors());
        assertEquals(5, group.getBehaviors().size());
        assertEquals(2, executors.stream().filter(FlatRandomRoamExecutor.class::equals).count());
        assertTrue(executors.contains(EntityBreedingExecutor.class));
        assertTrue(executors.contains(FollowEntityExecutor.class));
        assertTrue(executors.contains(LookAtEntityExecutor.class));
        assertEquals(List.of(1, 2, 4, 5, 6), group.getBehaviors().stream()
                .map(behavior -> behavior.getPriority())
                .sorted()
                .toList());
    }

    @Test
    void feedingAFoxStartsItsBreedingCycle() {
        var fox = createFox();
        var player = mock(EntityPlayer.class);
        when(player.getRuntimeId()).thenReturn(42L);

        assertTrue(fox.onInteract(player, ItemTypes.SWEET_BERRIES.createItemStack()));
        verify(player).tryConsumeItemInHand();
        assertEquals(fox.getTick(), fox.getMemoryStorage().get(MemoryTypes.LAST_BE_FEED_TIME));
        assertEquals(42L, fox.getMemoryStorage().get(MemoryTypes.LAST_FEED_PLAYER));
    }

    @Test
    void foxUsesTheAdultVanillaCollisionBox() {
        var aabb = createFox().getBaseAABB();

        assertEquals(0.6, aabb.maxX() - aabb.minX(), 1e-9);
        assertEquals(0.7, aabb.maxY() - aabb.minY(), 1e-9);
    }

    private static List<Class<?>> executors(Iterable<? extends org.allaymc.api.entity.ai.behavior.Behavior> behaviors) {
        var result = new java.util.ArrayList<Class<?>>();
        for (var behavior : behaviors) {
            result.add(assertInstanceOf(BehaviorImpl.class, behavior).getExecutor().getClass());
        }
        return List.copyOf(result);
    }

    private static EntityFox createFox() {
        return EntityTypes.FOX.createEntity(EntityInitInfo.builder().build());
    }
}
