package org.allaymc.server.entity.type;

import lombok.experimental.UtilityClass;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.interfaces.EntityAnimal;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.dimension.DimensionTypes;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.allaymc.server.entity.ai.evaluator.MemoryCheckNotEmptyEvaluator;
import org.allaymc.server.entity.ai.evaluator.PassByTimeEvaluator;
import org.allaymc.server.entity.ai.executor.EntityBreedingExecutor;
import org.allaymc.server.entity.ai.executor.EntityControlHelper;
import org.allaymc.server.entity.ai.executor.FleeFromTargetExecutor;
import org.allaymc.server.entity.ai.executor.MobMeleeAttackExecutor;
import org.allaymc.server.entity.ai.executor.ProjectileAttackExecutor;
import org.allaymc.server.entity.ai.executor.RamAttackExecutor;
import org.allaymc.server.entity.ai.executor.SpaceRandomRoamExecutor;
import org.allaymc.server.entity.ai.route.posevaluator.FlyingPosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.SwimmingPosEvaluator;
import org.allaymc.server.entity.ai.sensor.NearestFeedingPlayerSensor;
import org.allaymc.server.entity.ai.sensor.NearestPlayerSensor;
import org.allaymc.server.entity.component.EntityAIComponentImpl;
import org.allaymc.server.entity.component.EntityAgeComponentImpl;
import org.allaymc.server.entity.component.EntityAquaticPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityBabyComponentImpl;
import org.allaymc.server.entity.component.EntityFlyingPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityHeadYawComponentImpl;
import org.allaymc.server.entity.component.EntityLivingComponentImpl;
import org.allaymc.server.entity.component.EntityMobPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityParallelTickComponentImpl;
import org.allaymc.server.entity.component.EntityPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityUndeadComponentImpl;
import org.allaymc.server.entity.component.animal.EntityAnimalComponentImpl;
import org.allaymc.server.entity.component.animal.EntityAnimalPhysicsComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityHumanPhysicsComponentImpl;
import org.allaymc.server.entity.component.mob.EntityArmadilloBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityGoatBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityMobBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityMobLivingComponentImpl;
import org.allaymc.server.entity.component.mob.EntityMooshroomBaseComponentImpl;
import org.allaymc.server.entity.component.mob.LootContext;
import org.allaymc.server.entity.component.mob.MobLoot;
import org.allaymc.server.entity.component.projectile.EntityLlamaSpitPhysicsComponentImpl;
import org.allaymc.server.entity.component.projectile.EntityProjectileBaseComponentImpl;
import org.allaymc.server.entity.component.projectile.EntityProjectileComponentImpl;
import org.allaymc.server.entity.data.EntityId;
import org.allaymc.server.entity.impl.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.allaymc.server.entity.ai.evaluator.LogicHelper.all;
import static org.allaymc.server.entity.type.MobBehaviors.behavior;
import static org.allaymc.server.entity.type.MobBehaviors.flying;
import static org.allaymc.server.entity.type.MobBehaviors.ground;
import static org.allaymc.server.entity.type.MobBehaviors.lookAtPlayer;
import static org.allaymc.server.entity.type.MobBehaviors.roam;
import static org.allaymc.server.entity.type.MobBehaviors.spaceRoam;
import static org.allaymc.server.entity.type.MobBehaviors.targeting;

@SuppressWarnings("unused")
@UtilityClass
public final class PassiveMobEntityTypeInitializer {

    private static final Predicate<ItemStack> NOT_BREEDABLE = item -> false;

    public static void initHoglin() {
        EntityTypes.HOGLIN = animal(EntityHoglinImpl.class, EntityId.HOGLIN,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.4)
                        .convertsTo(() -> EntityTypes.ZOGLIN, 300,
                                entity -> entity.getDimension().getDimensionType() != DimensionTypes.NETHER,
                                SoundNames.MOB_HOGLIN_CONVERTED_TO_ZOMBIFIED),
                () -> new EntityMobLivingComponentImpl(40, PassiveMobEntityTypeInitializer::hoglinLoot, MobLoot.HOSTILE_XP),
                items(() -> ItemTypes.CRIMSON_FUNGUS))
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(PassiveMobEntityTypeInitializer::hoglinBehavior, EntityAIComponentImpl.class)
                .build();
    }

    private static EntityAIComponentImpl hoglinBehavior() {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestFeedingPlayerSensor(8))
                .sensor(new NearestPlayerSensor(16, 0, 20))
                .coreBehavior(MobBehaviors.inLove())
                .behavior(MobBehaviors.panic(0.2f))
                .behavior(behavior(new EntityBreedingExecutor(100, 0.15f),
                        entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE), 7))
                .behavior(behavior(hoglinAttack(MemoryTypes.ATTACK_TARGET, true),
                        targeting(MemoryTypes.ATTACK_TARGET, PassiveMobEntityTypeInitializer::isAdult), 8))
                .behavior(behavior(hoglinAttack(MemoryTypes.NEAREST_PLAYER, false),
                        targeting(MemoryTypes.NEAREST_PLAYER, PassiveMobEntityTypeInitializer::isAdult), 5))
                .behavior(lookAtPlayer())
                .behavior(roam(0.1f, 12));
        return new EntityAIComponentImpl(ground(builder).build());
    }

    private static MobMeleeAttackExecutor hoglinAttack(MemoryType<Long> memory, boolean clear) {
        return new MobMeleeAttackExecutor(memory, 0.135f, 32, clear, 40, 2.2, 3, 9, null).knockback(0.5, 0.6);
    }

    public static void initMooshroom() {
        EntityTypes.MOOSHROOM = animal(EntityMooshroomImpl.class, EntityId.MOOSHROOM,
                EntityMooshroomBaseComponentImpl::new,
                () -> new EntityMobLivingComponentImpl(10, PassiveMobEntityTypeInitializer::cowLoot, MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.WHEAT))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(0.1f), EntityAIComponentImpl.class)
                .build();
    }

    public static void initGoat() {
        EntityTypes.GOAT = animal(EntityGoatImpl.class, EntityId.GOAT,
                EntityGoatBaseComponentImpl::new,
                () -> new EntityMobLivingComponentImpl(10, MobLoot.NONE, MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.WHEAT))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAIComponentImpl(ground(MobBehaviors.animal(0.14f)
                        .behavior(behavior(new RamAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.3f),
                                targeting(MemoryTypes.NEAREST_PLAYER, PassiveMobEntityTypeInitializer::isAdult,
                                        entity -> RamAttackExecutor.canRam(entity, MemoryTypes.NEAREST_PLAYER)), 7)))
                        .build()), EntityAIComponentImpl.class)
                .build();
    }

    public static void initOcelot() {
        EntityTypes.OCELOT = animal(EntityOcelotImpl.class, EntityId.OCELOT,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.6, 0.7),
                () -> new EntityMobLivingComponentImpl(10, MobLoot.NONE, MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.COD, () -> ItemTypes.SALMON))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAIComponentImpl(ground(MobBehaviors.animal(0.12f)
                        .behavior(behavior(new FleeFromTargetExecutor(MemoryTypes.NEAREST_PLAYER, 0.25f, 5, 20),
                                all(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                        entity -> entity.getMemoryStorage().get(MemoryTypes.NEAREST_FEEDING_PLAYER) == null,
                                        entity -> nearestPlayerWithin(entity, 6)), 3)))
                        .build()), EntityAIComponentImpl.class)
                .build();
    }

    public static void initPanda() {
        EntityTypes.PANDA = animal(EntityPandaImpl.class, EntityId.PANDA,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.3, 1.25)
                        .babyScale(0.4f)
                        .variant(PassiveMobEntityTypeInitializer::rollPandaPersonality),
                () -> new EntityMobLivingComponentImpl(20, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.BAMBOO, context.count(0, 2)), MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.BAMBOO))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAIComponentImpl(ground(MobBehaviors.animal(0.06f)
                        .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.1f, 32, true, 20, 2.0, 6, null),
                                targeting(MemoryTypes.ATTACK_TARGET, PassiveMobEntityTypeInitializer::isAdult,
                                        entity -> variantOf(entity) == 6), 7)))
                        .build()), EntityAIComponentImpl.class)
                .build();
    }

    private static int rollPandaPersonality() {
        var rand = ThreadLocalRandom.current();
        if (rand.nextInt(50) == 0) {
            return 4;
        }
        return switch (rand.nextInt(8)) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 6;
            default -> 0;
        };
    }

    public static void initParrot() {
        EntityTypes.PARROT = animal(EntityParrotImpl.class, EntityId.PARROT,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.5, 1.0)
                        .variant(() -> ThreadLocalRandom.current().nextInt(5)),
                () -> new EntityMobLivingComponentImpl(6, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.FEATHER, context.random().nextInt(1, 3)), MobLoot.ANIMAL_XP)
                        .immuneTo(DamageType.FALL),
                NOT_BREEDABLE)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            .behavior(lookAtPlayer())
                            .behavior(spaceRoam(0.1f, 10, 4, 60, new FlyingPosEvaluator()));
                    return new EntityAIComponentImpl(flying(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initPolarBear() {
        EntityTypes.POLAR_BEAR = animal(EntityPolarBearImpl.class, EntityId.POLAR_BEAR,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.4),
                () -> new EntityMobLivingComponentImpl(30, PassiveMobEntityTypeInitializer::polarBearLoot, MobLoot.ANIMAL_XP),
                NOT_BREEDABLE)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAIComponentImpl(ground(MobBehaviors.animal(0.1f)
                        .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.16f, 32, true, 20, 2.2, 6, null),
                                targeting(MemoryTypes.ATTACK_TARGET, PassiveMobEntityTypeInitializer::isAdult), 7)))
                        .build()), EntityAIComponentImpl.class)
                .build();
    }

    public static void initTurtle() {
        EntityTypes.TURTLE = animal(EntityTurtleImpl.class, EntityId.TURTLE,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.2, 0.4).babyScale(0.16f),
                () -> new EntityMobLivingComponentImpl(30, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.SEAGRASS, context.count(0, 2)), MobLoot.ANIMAL_XP)
                        .breathing(EntityMobLivingComponentImpl.Breathing.AMPHIBIOUS),
                items(() -> ItemTypes.SEAGRASS))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(0.04f), EntityAIComponentImpl.class)
                .build();
    }

    public static void initArmadillo() {
        EntityTypes.ARMADILLO = animal(EntityArmadilloImpl.class, EntityId.ARMADILLO,
                EntityArmadilloBaseComponentImpl::new,
                () -> new EntityMobLivingComponentImpl(12, MobLoot.NONE, MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.SPIDER_EYE))
                .setProperties(EntityPropertyTypes.ARMADILLO_STATE)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAIComponentImpl(ground(MobBehaviors.animal(0.056f)
                        .behavior(behavior(entity -> {
                            EntityControlHelper.removeRouteTarget(entity);
                            return isRolledArmadillo(entity);
                        }, PassiveMobEntityTypeInitializer::isRolledArmadillo, 9)))
                        .build()), EntityAIComponentImpl.class)
                .build();
    }

    private static boolean isRolledArmadillo(EntityIntelligent entity) {
        return entity instanceof EntityImpl impl && impl.getBaseComponent() instanceof EntityArmadilloBaseComponentImpl armadillo
               && armadillo.isRolled();
    }

    public static void initSniffer() {
        EntityTypes.SNIFFER = animal(EntitySnifferImpl.class, EntityId.SNIFFER,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.9, 1.75)
                        .babyScale(0.45f)
                        .periodicDrop(() -> ThreadLocalRandom.current().nextInt(6000, 12001),
                                () -> (ThreadLocalRandom.current().nextBoolean() ? ItemTypes.TORCHFLOWER_SEEDS : ItemTypes.PITCHER_POD).createItemStack(1),
                                SoundNames.MOB_SNIFFER_DROP_SEED),
                () -> new EntityMobLivingComponentImpl(14, MobLoot.NONE, MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.TORCHFLOWER_SEEDS))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(0.05f), EntityAIComponentImpl.class)
                .build();
    }

    public static void initCamels() {
        EntityTypes.CAMEL = animal(EntityCamelImpl.class, EntityId.CAMEL,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.7, 2.375).babyScale(0.45f),
                () -> new EntityMobLivingComponentImpl(32, MobLoot.NONE, MobLoot.ANIMAL_XP),
                items(() -> ItemTypes.CACTUS))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(0.06f), EntityAIComponentImpl.class)
                .build();

        EntityTypes.CAMEL_HUSK = animal(EntityCamelHuskImpl.class, EntityId.CAMEL_HUSK,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.7, 2.375),
                () -> new EntityMobLivingComponentImpl(32, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.ROTTEN_FLESH, context.count(2, 3)), MobLoot.ANIMAL_XP),
                NOT_BREEDABLE)
                .addComponent(PassiveMobEntityTypeInitializer::sunproofUndead, EntityUndeadComponentImpl.class)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(0.06f), EntityAIComponentImpl.class)
                .build();
    }

    public static void initLlamas() {
        EntityTypes.LLAMA_SPIT = AllayEntityType
                .builder(EntityLlamaSpitImpl.class)
                .vanillaEntity(EntityId.LLAMA_SPIT)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityLlamaSpitPhysicsComponentImpl::new, EntityLlamaSpitPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();

        EntityTypes.LLAMA = llama(EntityLlamaImpl.class, EntityId.LLAMA, 0);
        EntityTypes.TRADER_LLAMA = llama(EntityTraderLlamaImpl.class, EntityId.TRADER_LLAMA, 1);
    }

    private static <T extends Entity> EntityType<T> llama(
            Class<? extends EntityImpl> implClass, EntityId id, int decor) {
        return animal(implClass, id,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.9, 1.87)
                        .variant(() -> ThreadLocalRandom.current().nextInt(4))
                        .markVariant(() -> decor),
                () -> new EntityMobLivingComponentImpl(30, PassiveMobEntityTypeInitializer::leatherLoot, MobLoot.ANIMAL_XP).randomHealth(15),
                NOT_BREEDABLE)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAIComponentImpl(ground(MobBehaviors.animal(0.1f)
                        .behavior(behavior(new ProjectileAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.1f, 32, 16, 0, true, 40,
                                        () -> EntityTypes.LLAMA_SPIT, 1.5, SoundNames.MOB_LLAMA_SPIT),
                                targeting(MemoryTypes.ATTACK_TARGET), 7)))
                        .build()), EntityAIComponentImpl.class)
                .build();
    }

    public static void initHorses() {
        EntityTypes.HORSE = horse(EntityHorseImpl.class, EntityId.HORSE,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.6)
                        .variant(() -> ThreadLocalRandom.current().nextInt(7))
                        .markVariant(() -> ThreadLocalRandom.current().nextInt(5)),
                () -> new EntityMobLivingComponentImpl(30, PassiveMobEntityTypeInitializer::leatherLoot, MobLoot.ANIMAL_XP).randomHealth(15),
                null, 0.09f);
        EntityTypes.DONKEY = horse(EntityDonkeyImpl.class, EntityId.DONKEY,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.6),
                () -> new EntityMobLivingComponentImpl(30, PassiveMobEntityTypeInitializer::leatherLoot, MobLoot.ANIMAL_XP).randomHealth(15),
                null, 0.07f);
        EntityTypes.MULE = horse(EntityMuleImpl.class, EntityId.MULE,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.6),
                () -> new EntityMobLivingComponentImpl(30, PassiveMobEntityTypeInitializer::leatherLoot, MobLoot.ANIMAL_XP).randomHealth(15),
                null, 0.07f);
        EntityTypes.SKELETON_HORSE = horse(EntitySkeletonHorseImpl.class, EntityId.SKELETON_HORSE,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.6),
                () -> new EntityMobLivingComponentImpl(15, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.BONE, context.count(0, 2)), MobLoot.ANIMAL_XP)
                        .breathing(EntityMobLivingComponentImpl.Breathing.AMPHIBIOUS),
                PassiveMobEntityTypeInitializer::sunproofUndead, 0.08f);
        EntityTypes.ZOMBIE_HORSE = horse(EntityZombieHorseImpl.class, EntityId.ZOMBIE_HORSE,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.6),
                () -> new EntityMobLivingComponentImpl(25, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.ROTTEN_FLESH, context.count(2, 3)), MobLoot.ANIMAL_XP),
                EntityUndeadComponentImpl::new, 0.095f);
    }

    private static <T extends Entity> EntityType<T> horse(
            Class<? extends EntityImpl> implClass, EntityId id, Function<EntityInitInfo, EntityMobBaseComponentImpl> base,
            Supplier<EntityMobLivingComponentImpl> living, Supplier<EntityUndeadComponentImpl> undead, float roamSpeed) {
        var builder = animal(implClass, id, base, living, NOT_BREEDABLE)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(roamSpeed), EntityAIComponentImpl.class);
        if (undead != null) {
            builder.addComponent(undead, EntityUndeadComponentImpl.class);
        }
        return builder.build();
    }

    public static void initStrider() {
        EntityTypes.STRIDER = animal(EntityStriderImpl.class, EntityId.STRIDER,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.9, 1.7),
                () -> new EntityMobLivingComponentImpl(20, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.STRING, context.count(2, 5)), MobLoot.ANIMAL_XP).fireproof(),
                items(() -> ItemTypes.WARPED_FUNGUS))
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> MobBehaviors.groundAnimal(0.064f), EntityAIComponentImpl.class)
                .build();
    }

    public static void initHappyGhast() {
        EntityTypes.HAPPY_GHAST = animal(EntityHappyGhastImpl.class, EntityId.HAPPY_GHAST,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 4.0, 4.0).babyScale(0.2375f),
                () -> new EntityMobLivingComponentImpl(20, MobLoot.NONE, MobLoot.ANIMAL_XP)
                        .immuneTo(DamageType.FALL)
                        .breathing(EntityMobLivingComponentImpl.Breathing.AMPHIBIOUS),
                NOT_BREEDABLE)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            .behavior(lookAtPlayer())
                            .behavior(spaceRoam(0.03f, 16, 6, 120, new FlyingPosEvaluator()));
                    return new EntityAIComponentImpl(flying(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initNautiluses() {
        EntityTypes.NAUTILUS = animal(EntityNautilusImpl.class, EntityId.NAUTILUS,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.875, 0.95),
                () -> new EntityMobLivingComponentImpl(15, (context, drops) -> {
                    if (context.killedByPlayer() && context.chance(0.05, 0.01)) {
                        MobLoot.add(drops, ItemTypes.NAUTILUS_SHELL, 1);
                    }
                }, MobLoot.ANIMAL_XP).breathing(EntityMobLivingComponentImpl.Breathing.WATER).immuneTo(DamageType.FALL),
                items(() -> ItemTypes.COD, () -> ItemTypes.SALMON, () -> ItemTypes.PUFFERFISH, () -> ItemTypes.TROPICAL_FISH,
                        () -> ItemTypes.COOKED_COD, () -> ItemTypes.COOKED_SALMON, () -> ItemTypes.COD_BUCKET,
                        () -> ItemTypes.SALMON_BUCKET, () -> ItemTypes.PUFFERFISH_BUCKET, () -> ItemTypes.TROPICAL_FISH_BUCKET))
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(() -> swimmer(0.15f, true), EntityAIComponentImpl.class)
                .build();

        EntityTypes.ZOMBIE_NAUTILUS = animal(EntityZombieNautilusImpl.class, EntityId.ZOMBIE_NAUTILUS,
                initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.875, 0.95),
                () -> new EntityMobLivingComponentImpl(15, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.ROTTEN_FLESH, context.count(0, 3)), MobLoot.ANIMAL_XP)
                        .breathing(EntityMobLivingComponentImpl.Breathing.AMPHIBIOUS).immuneTo(DamageType.FALL),
                NOT_BREEDABLE)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(() -> swimmer(0.12f, false), EntityAIComponentImpl.class)
                .build();
    }

    public static void initSquids() {
        EntityTypes.SQUID = aquatic(EntitySquidImpl.class, EntityId.SQUID, 0.8, 0.8,
                () -> new EntityMobLivingComponentImpl(10, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.INK_SAC, context.count(1, 3)), MobLoot.ANIMAL_XP)
                        .breathing(EntityMobLivingComponentImpl.Breathing.WATER).immuneTo(DamageType.FALL),
                0.12f);
        EntityTypes.GLOW_SQUID = aquatic(EntityGlowSquidImpl.class, EntityId.GLOW_SQUID, 0.8, 0.8,
                () -> new EntityMobLivingComponentImpl(10, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.GLOW_INK_SAC, context.count(1, 3)), MobLoot.ANIMAL_XP)
                        .breathing(EntityMobLivingComponentImpl.Breathing.WATER).immuneTo(DamageType.FALL),
                0.12f);
    }

    public static void initDolphin() {
        EntityTypes.DOLPHIN = AllayEntityType
                .builder(EntityDolphinImpl.class)
                .vanillaEntity(EntityId.DOLPHIN)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.9, 0.6), EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(10, (context, drops) ->
                                MobLoot.addCooked(drops, context, ItemTypes.COD, ItemTypes.COOKED_COD, context.count(0, 1)), MobLoot.ANIMAL_XP)
                                .breathing(EntityMobLivingComponentImpl.Breathing.AMPHIBIOUS).immuneTo(DamageType.FALL),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var evaluator = new SwimmingPosEvaluator();
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.3f, 32, true, 20, 1.6, 3, null),
                                    targeting(MemoryTypes.ATTACK_TARGET), 3))
                            .behavior(spaceRoam(0.25f, 12, 4, 40, evaluator));
                    return new EntityAIComponentImpl(MobBehaviors.swimming(builder, evaluator).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initTadpole() {
        EntityTypes.TADPOLE = AllayEntityType
                .builder(EntityTadpoleImpl.class)
                .vanillaEntity(EntityId.TADPOLE)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.8, 0.6)
                        .convertsTo(() -> EntityTypes.FROG, 24000, entity -> true, null), EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(6, MobLoot.NONE, MobLoot.NO_XP)
                                .breathing(EntityMobLivingComponentImpl.Breathing.WATER).immuneTo(DamageType.FALL),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> swimmer(0.1f, false), EntityAIComponentImpl.class)
                .build();
    }

    public static void initVillagers() {
        EntityTypes.VILLAGER = villager(EntityVillagerImpl.class, EntityId.VILLAGER, true);
        // Zombie villagers cure into villager_v2. Variant is the profession (0–14) and
        // mark variant is the biome (0–6), matching the zombie villager that converts into it.
        EntityTypes.VILLAGER_V2 = villager(EntityVillagerV2Impl.class, EntityId.VILLAGER_V2,
                () -> ThreadLocalRandom.current().nextInt(15),
                () -> ThreadLocalRandom.current().nextInt(7));
        EntityTypes.WANDERING_TRADER = villager(EntityWanderingTraderImpl.class, EntityId.WANDERING_TRADER, false);
    }

    private static <T extends Entity> EntityType<T> villager(
            Class<? extends EntityImpl> implClass, EntityId id, boolean profession) {
        return villager(implClass, id, profession ? () -> ThreadLocalRandom.current().nextInt(6) : null, null);
    }

    private static <T extends Entity> EntityType<T> villager(
            Class<? extends EntityImpl> implClass, EntityId id,
            IntSupplier variantRoller, IntSupplier markVariantRoller) {
        return AllayEntityType
                .builder(implClass)
                .vanillaEntity(id)
                .addComponent(initInfo -> {
                    var base = new EntityMobBaseComponentImpl(initInfo, 0.6, 1.9);
                    if (variantRoller != null) {
                        base.variant(variantRoller);
                    }
                    if (markVariantRoller != null) {
                        base.markVariant(markVariantRoller);
                    }
                    return base;
                }, EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(20, MobLoot.NONE, MobLoot.NO_XP), EntityMobLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            .behavior(MobBehaviors.panic(0.2f))
                            .behavior(lookAtPlayer())
                            .behavior(roam(0.08f, 12));
                    return new EntityAIComponentImpl(ground(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initNpc() {
        EntityTypes.NPC = AllayEntityType
                .builder(EntityNpcImpl.class)
                .vanillaEntity(EntityId.NPC)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.6, 2.1), EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityLivingComponentImpl() {
                    @Override
                    public boolean canBeAttacked(DamageContainer damage) {
                        return damage.getDamageType() == DamageType.API || damage.getDamageType() == DamageType.VOID;
                    }
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityPhysicsComponentImpl::new, EntityPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .build();
    }

    private static AllayEntityType.Builder animal(Class<? extends EntityImpl> implClass, EntityId id,
                                                  Function<EntityInitInfo, ? extends EntityMobBaseComponentImpl> base,
                                                  Supplier<? extends EntityMobLivingComponentImpl> living,
                                                  Predicate<ItemStack> breedingItems) {
        return AllayEntityType
                .builder(implClass)
                .vanillaEntity(id)
                .addComponent(base, EntityMobBaseComponentImpl.class)
                .addComponent(living, EntityMobLivingComponentImpl.class)
                .addComponent(() -> new EntityAnimalComponentImpl(breedingItems), EntityAnimalComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class);
    }

    private static <T extends Entity> EntityType<T> aquatic(
            Class<? extends EntityImpl> implClass, EntityId id, double width, double height,
            Supplier<EntityMobLivingComponentImpl> living, float speed) {
        return AllayEntityType
                .builder(implClass)
                .vanillaEntity(id)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, width, height), EntityMobBaseComponentImpl.class)
                .addComponent(living, EntityMobLivingComponentImpl.class)
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> swimmer(speed, false), EntityAIComponentImpl.class)
                .build();
    }

    private static EntityAIComponentImpl swimmer(float speed, boolean breeds) {
        var evaluator = new SwimmingPosEvaluator();
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(8, 0, 20))
                .behavior(behavior(new SpaceRandomRoamExecutor(speed * 2.5f, 10, 4, 1, 12, evaluator),
                        new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100), 6))
                .behavior(spaceRoam(speed, 10, 4, 40, evaluator));
        if (breeds) {
            builder.sensor(new NearestFeedingPlayerSensor(8))
                    .coreBehavior(MobBehaviors.inLove())
                    .behavior(behavior(new EntityBreedingExecutor(100, speed * 1.5f),
                            entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE), 5));
        }
        return new EntityAIComponentImpl(MobBehaviors.swimming(builder, evaluator).build());
    }

    @SafeVarargs
    private static Predicate<ItemStack> items(Supplier<ItemType<?>>... types) {
        return item -> {
            var type = item.getItemType();
            for (var supplier : types) {
                if (supplier.get() == type) {
                    return true;
                }
            }
            return false;
        };
    }

    private static boolean isAdult(EntityIntelligent entity) {
        return !(entity instanceof EntityAnimal animal) || !animal.isBaby();
    }

    private static int variantOf(EntityIntelligent entity) {
        return entity instanceof EntityImpl impl && impl.getBaseComponent() instanceof EntityMobBaseComponentImpl base
                ? base.getVariant() : 0;
    }

    private static boolean nearestPlayerWithin(EntityIntelligent entity, double range) {
        var targetId = entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        return target != null && entity.getLocation().distanceSquared(target.getLocation()) <= range * range;
    }

    private static EntityUndeadComponentImpl sunproofUndead() {
        return new EntityUndeadComponentImpl() {
            @Override
            public boolean ignitedBySunlight() {
                return false;
            }
        };
    }

    private static void hoglinLoot(LootContext context, List<ItemStack> drops) {
        if (context.baby()) {
            return;
        }
        MobLoot.addCooked(drops, context, ItemTypes.PORKCHOP, ItemTypes.COOKED_PORKCHOP, context.count(2, 4));
        MobLoot.add(drops, ItemTypes.LEATHER, context.count(0, 1));
    }

    private static void cowLoot(LootContext context, List<ItemStack> drops) {
        if (context.baby()) {
            return;
        }
        MobLoot.add(drops, ItemTypes.LEATHER, context.count(0, 2));
        MobLoot.addCooked(drops, context, ItemTypes.BEEF, ItemTypes.COOKED_BEEF, context.count(1, 3));
    }

    private static void polarBearLoot(LootContext context, List<ItemStack> drops) {
        if (context.baby()) {
            return;
        }
        MobLoot.addCooked(drops, context, ItemTypes.COD, ItemTypes.COOKED_COD, context.count(0, 2));
        MobLoot.addCooked(drops, context, ItemTypes.SALMON, ItemTypes.COOKED_SALMON, context.count(0, 2));
    }

    private static void leatherLoot(LootContext context, List<ItemStack> drops) {
        if (context.baby()) {
            return;
        }
        MobLoot.add(drops, ItemTypes.LEATHER, context.count(0, 2));
    }
}
