package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityAnimalComponent;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityFoxBaseComponent;
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
import org.allaymc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
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
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertInstanceOf(EntityFoxBaseComponent.class, fox);
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

    @Test
    void foxSleepingPoseCanBeToggledWithoutABed() {
        var fox = createFox();

        assertFalse(fox.isSleeping());
        fox.setSleeping(true);
        assertTrue(fox.isSleeping());
        fox.setSleeping(true);
        assertTrue(fox.isSleeping());
        fox.setSleeping(false);
        assertFalse(fox.isSleeping());
    }

    @Test
    void manualControlInterruptsAutonomousBehaviorsButKeepsControllersRunning() {
        var fox = createFox();
        var starts = new AtomicInteger();
        var executions = new AtomicInteger();
        var interruptions = new AtomicInteger();
        var controllerTicks = new AtomicInteger();
        var autonomousMoveTarget = new Vector3d(-4, 2, -8);
        var autonomousLookTarget = new Vector3d(-2, 3, -6);

        var normalExecutor = trackingExecutor(starts, executions, interruptions, () -> {
            fox.setMoveTarget(autonomousMoveTarget);
            fox.setLookTarget(autonomousLookTarget);
        });
        var coreExecutor = trackingExecutor(starts, executions, interruptions, () -> {
        });
        var behaviorGroup = BehaviorGroupImpl.builder()
                .coreBehavior(BehaviorImpl.builder()
                        .executor(coreExecutor)
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(normalExecutor)
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                .controller(entity -> {
                    controllerTicks.incrementAndGet();
                    return true;
                })
                .build();
        fox.setBehaviorGroup(behaviorGroup);
        fox.setPitchEnabled(false);

        behaviorGroup.tick();
        assertEquals(2, starts.get());
        assertEquals(2, executions.get());
        assertEquals(1, controllerTicks.get());
        assertPosition(autonomousMoveTarget, fox.getMoveTarget());

        var suppliedMoveTarget = new Vector3d(12, 4, 7);
        var suppliedLookTarget = new Vector3d(10, 5, 6);
        fox.setManualControlEnabled(true);
        fox.navigateTo(suppliedMoveTarget, 0.24f);
        fox.lookAt(suppliedLookTarget);
        suppliedMoveTarget.set(100, 100, 100);
        suppliedLookTarget.set(100, 100, 100);

        behaviorGroup.tick();
        assertTrue(fox.isManualControlEnabled());
        assertEquals(2, interruptions.get());
        assertEquals(2, executions.get());
        assertEquals(2, controllerTicks.get());
        assertEquals(0.24f, fox.getMovementSpeed());
        assertTrue(fox.isPitchEnabled());
        assertPosition(new Vector3d(12, 4, 7), fox.getMoveTarget());
        assertPosition(new Vector3d(10, 5, 6), fox.getLookTarget());

        behaviorGroup.tick();
        assertEquals(2, interruptions.get());
        assertEquals(2, executions.get());
        assertEquals(3, controllerTicks.get());

        fox.stopNavigation();
        fox.stopLooking();
        behaviorGroup.tick();
        assertNull(fox.getMoveTarget());
        assertNull(fox.getLookTarget());
        assertFalse(fox.hasMoveDirection());
        assertEquals(0.1f, fox.getMovementSpeed());
        assertFalse(fox.isPitchEnabled());
        assertEquals(4, controllerTicks.get());

        fox.setManualControlEnabled(false);
        behaviorGroup.tick();
        assertFalse(fox.isManualControlEnabled());
        assertEquals(4, starts.get());
        assertEquals(4, executions.get());
        assertEquals(5, controllerTicks.get());
        assertEquals(0.1f, fox.getMovementSpeed());
        assertFalse(fox.isPitchEnabled());
        assertPosition(autonomousMoveTarget, fox.getMoveTarget());
        assertPosition(autonomousLookTarget, fox.getLookTarget());
    }

    @Test
    void manualControlRejectsInvalidCommands() {
        var fox = createFox();

        assertThrows(IllegalStateException.class, () -> fox.navigateTo(new Vector3d(), 0.2f));
        assertThrows(IllegalStateException.class, () -> fox.lookAt(new Vector3d()));

        fox.setManualControlEnabled(true);
        assertThrows(NullPointerException.class, () -> fox.navigateTo(null, 0.2f));
        assertThrows(IllegalArgumentException.class, () -> fox.navigateTo(new Vector3d(), 0));
        assertThrows(IllegalArgumentException.class, () -> fox.navigateTo(new Vector3d(), Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> fox.navigateTo(new Vector3d(Double.POSITIVE_INFINITY, 0, 0), 0.2f));
        assertThrows(NullPointerException.class, () -> fox.lookAt(null));
        assertThrows(IllegalArgumentException.class,
                () -> fox.lookAt(new Vector3d(0, Double.NEGATIVE_INFINITY, 0)));
    }

    private static BehaviorExecutor trackingExecutor(AtomicInteger starts, AtomicInteger executions,
                                                     AtomicInteger interruptions, Runnable onStart) {
        return new BehaviorExecutor() {
            @Override
            public boolean execute(EntityIntelligent entity) {
                executions.incrementAndGet();
                return true;
            }

            @Override
            public void onStart(EntityIntelligent entity) {
                starts.incrementAndGet();
                onStart.run();
            }

            @Override
            public void onInterrupt(EntityIntelligent entity) {
                interruptions.incrementAndGet();
            }
        };
    }

    private static void assertPosition(Vector3dc expected, Vector3dc actual) {
        assertNotNull(actual);
        assertEquals(expected.x(), actual.x());
        assertEquals(expected.y(), actual.y());
        assertEquals(expected.z(), actual.z());
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
