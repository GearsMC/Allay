package org.allaymc.server.entity.type;

import org.allaymc.api.entity.ai.behavior.Behavior;
import org.allaymc.api.entity.ai.behavior.BehaviorEvaluator;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.server.entity.ai.behavior.BehaviorImpl;
import org.allaymc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.allaymc.server.entity.ai.controller.FluctuateController;
import org.allaymc.server.entity.ai.controller.FlyController;
import org.allaymc.server.entity.ai.controller.LookController;
import org.allaymc.server.entity.ai.controller.SwimController;
import org.allaymc.server.entity.ai.controller.WalkController;
import org.allaymc.server.entity.ai.evaluator.MemoryCheckNotEmptyEvaluator;
import org.allaymc.server.entity.ai.evaluator.PassByTimeEvaluator;
import org.allaymc.server.entity.ai.evaluator.ProbabilityEvaluator;
import org.allaymc.server.entity.ai.executor.EntityBreedingExecutor;
import org.allaymc.server.entity.ai.executor.FlatRandomRoamExecutor;
import org.allaymc.server.entity.ai.executor.FollowEntityExecutor;
import org.allaymc.server.entity.ai.executor.InLoveExecutor;
import org.allaymc.server.entity.ai.executor.LookAtEntityExecutor;
import org.allaymc.server.entity.ai.executor.SpaceRandomRoamExecutor;
import org.allaymc.server.entity.ai.route.finder.FlatAStarRouteFinder;
import org.allaymc.server.entity.ai.route.finder.SpaceAStarRouteFinder;
import org.allaymc.server.entity.ai.route.posevaluator.FlyingPosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.SpacePosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.SwimmingPosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.WalkingPosEvaluator;
import org.allaymc.server.entity.ai.sensor.NearestFeedingPlayerSensor;
import org.allaymc.server.entity.ai.sensor.NearestPlayerSensor;
import org.allaymc.server.entity.component.EntityAIComponentImpl;

import static org.allaymc.server.entity.ai.evaluator.LogicHelper.all;

final class MobBehaviors {

    static final float PANIC_FACTOR = 2.875f;
    static final float FOLLOW_FACTOR = 2.53f;
    static final float BREED_FACTOR = 2.3f;

    private MobBehaviors() {
    }

    @FunctionalInterface
    interface ExecutorFactory {
        BehaviorExecutor create(MemoryType<Long> targetMemory, boolean clearTargetAfterLose);
    }

    static boolean hasValidTarget(EntityIntelligent entity, MemoryType<Long> memory) {
        var targetId = entity.getMemoryStorage().get(memory);
        return targetId != null && EntityTypeInitializer.isValidHostileTarget(entity, targetId);
    }

    static BehaviorEvaluator targeting(MemoryType<Long> memory, BehaviorEvaluator... extra) {
        var evaluators = new BehaviorEvaluator[extra.length + 2];
        evaluators[0] = new MemoryCheckNotEmptyEvaluator(memory);
        evaluators[1] = entity -> hasValidTarget(entity, memory);
        System.arraycopy(extra, 0, evaluators, 2, extra.length);
        return all(evaluators);
    }

    static Behavior behavior(BehaviorExecutor executor, BehaviorEvaluator evaluator, int priority) {
        return BehaviorImpl.builder().executor(executor).evaluator(evaluator).priority(priority).build();
    }

    static Behavior behavior(BehaviorExecutor executor, BehaviorEvaluator evaluator, int priority, int period) {
        return BehaviorImpl.builder().executor(executor).evaluator(evaluator).priority(priority).period(period).build();
    }

    static Behavior roam(float speed, int range) {
        return behavior(new FlatRandomRoamExecutor(speed, range, 100, false, -1, true, 10), entity -> true, 1);
    }

    static Behavior spaceRoam(float speed, int range, int verticalRange, int frequency,
                              SpacePosEvaluator evaluator) {
        return behavior(new SpaceRandomRoamExecutor(speed, range, verticalRange, frequency, 12, evaluator), entity -> true, 1);
    }

    static Behavior panic(float speed) {
        return behavior(new FlatRandomRoamExecutor(speed, 12, 40, true, 100, true, 10),
                new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100), 6);
    }

    static Behavior lookAtPlayer() {
        return behavior(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100),
                all(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER), new ProbabilityEvaluator(2, 5)), 2, 100);
    }

    static Behavior inLove() {
        return BehaviorImpl.builder()
                .executor(new InLoveExecutor(400))
                .evaluator(all(
                        entity -> !entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE),
                        entity -> {
                            var lastLoveTime = entity.getMemoryStorage().get(MemoryTypes.LAST_IN_LOVE_TIME);
                            return lastLoveTime == null || lastLoveTime <= 0 || entity.getTick() - lastLoveTime >= 6000;
                        },
                        new PassByTimeEvaluator(MemoryTypes.LAST_BE_FEED_TIME, 0, 400)
                ))
                .priority(1)
                .build();
    }

    static BehaviorGroupImpl.BehaviorGroupImplBuilder ground(BehaviorGroupImpl.BehaviorGroupImplBuilder builder) {
        return builder
                .controller(new WalkController())
                .controller(new FluctuateController())
                .controller(new LookController(true, true))
                .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()));
    }

    static BehaviorGroupImpl.BehaviorGroupImplBuilder flying(BehaviorGroupImpl.BehaviorGroupImplBuilder builder) {
        return builder
                .controller(new FlyController())
                .controller(new LookController(true, true))
                .routeFinder(new SpaceAStarRouteFinder(new FlyingPosEvaluator()));
    }

    static BehaviorGroupImpl.BehaviorGroupImplBuilder swimming(BehaviorGroupImpl.BehaviorGroupImplBuilder builder,
                                                             SwimmingPosEvaluator evaluator) {
        return builder
                .controller(new SwimController())
                .controller(new LookController(true, true))
                .routeFinder(new SpaceAStarRouteFinder(evaluator));
    }

    static EntityAIComponentImpl hostile(double senseRange, float roamSpeed, ExecutorFactory factory) {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(senseRange, 0, 20))
                .behavior(behavior(factory.create(MemoryTypes.ATTACK_TARGET, true), targeting(MemoryTypes.ATTACK_TARGET), 3))
                .behavior(behavior(factory.create(MemoryTypes.NEAREST_PLAYER, false), targeting(MemoryTypes.NEAREST_PLAYER), 2))
                .behavior(roam(roamSpeed, 12));
        return new EntityAIComponentImpl(ground(builder).build());
    }

    static EntityAIComponentImpl neutral(float roamSpeed, ExecutorFactory factory) {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(16, 0, 20))
                .behavior(behavior(factory.create(MemoryTypes.ATTACK_TARGET, true), targeting(MemoryTypes.ATTACK_TARGET), 3))
                .behavior(lookAtPlayer())
                .behavior(roam(roamSpeed, 12));
        return new EntityAIComponentImpl(ground(builder).build());
    }

    static BehaviorGroupImpl.BehaviorGroupImplBuilder animal(float roamSpeed) {
        return BehaviorGroupImpl.builder()
                .sensor(new NearestFeedingPlayerSensor(8))
                .sensor(new NearestPlayerSensor(8, 0, 20))
                .coreBehavior(inLove())
                .behavior(panic(roamSpeed * PANIC_FACTOR))
                .behavior(behavior(new EntityBreedingExecutor(100, roamSpeed * BREED_FACTOR),
                        entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE), 5))
                .behavior(behavior(new FollowEntityExecutor(MemoryTypes.NEAREST_FEEDING_PLAYER, roamSpeed * FOLLOW_FACTOR, 64, 2.25),
                        new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER), 4))
                .behavior(lookAtPlayer())
                .behavior(roam(roamSpeed, 12));
    }

    static EntityAIComponentImpl groundAnimal(float roamSpeed) {
        return new EntityAIComponentImpl(ground(animal(roamSpeed)).build());
    }
}
