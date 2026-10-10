package org.allaymc.server.entity.type;

import lombok.experimental.UtilityClass;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityBreezeWindChargeProjectile;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityThrownTrident;
import org.allaymc.api.entity.interfaces.EntityWindChargeProjectile;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.data.PotionType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.allaymc.server.entity.ai.evaluator.MemoryCheckNotEmptyEvaluator;
import org.allaymc.server.entity.ai.executor.BreezeJumpExecutor;
import org.allaymc.server.entity.ai.executor.EntityControlHelper;
import org.allaymc.server.entity.ai.executor.EvokerSpellExecutor;
import org.allaymc.server.entity.ai.executor.FleeFromTargetExecutor;
import org.allaymc.server.entity.ai.executor.FollowEntityExecutor;
import org.allaymc.server.entity.ai.executor.GhastFireballAttackExecutor;
import org.allaymc.server.entity.ai.executor.GuardianLaserExecutor;
import org.allaymc.server.entity.ai.executor.MobMeleeAttackExecutor;
import org.allaymc.server.entity.ai.executor.ProjectileAttackExecutor;
import org.allaymc.server.entity.ai.executor.TippedBowAttackExecutor;
import org.allaymc.server.entity.ai.executor.WardenSonicBoomExecutor;
import org.allaymc.server.entity.ai.route.posevaluator.FlyingPosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.SwimmingPosEvaluator;
import org.allaymc.server.entity.ai.sensor.NearestMonsterSensor;
import org.allaymc.server.entity.ai.sensor.NearestPlayerSensor;
import org.allaymc.server.entity.component.EntityAIComponentImpl;
import org.allaymc.server.entity.component.EntityAquaticPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityBabyComponentImpl;
import org.allaymc.server.entity.component.EntityFlyingPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityHeadYawComponentImpl;
import org.allaymc.server.entity.component.EntityMobPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityParallelTickComponentImpl;
import org.allaymc.server.entity.component.EntitySpiderPhysicsComponentImpl;
import org.allaymc.server.entity.component.EntityUndeadComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityArmedBaseComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityHumanLikeContainerHolderComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityHumanPhysicsComponentImpl;
import org.allaymc.server.entity.component.mob.EntityBoggedBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityCaveSpiderBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityCreakingBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityEvocationFangBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityEvokerBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityGhastBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityGuardianBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityGuardianLivingComponentImpl;
import org.allaymc.server.entity.component.mob.EntityMobBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityMobLivingComponentImpl;
import org.allaymc.server.entity.component.mob.EntitySnowGolemBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityVexBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityWardenBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityWardenLivingComponentImpl;
import org.allaymc.server.entity.component.mob.EntityZombieVillagerBaseComponentImpl;
import org.allaymc.server.entity.component.mob.LootContext;
import org.allaymc.server.entity.component.mob.MobLoot;
import org.allaymc.server.entity.data.EntityId;
import org.allaymc.server.entity.impl.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.allaymc.server.entity.ai.evaluator.LogicHelper.all;
import static org.allaymc.server.entity.type.MobBehaviors.behavior;
import static org.allaymc.server.entity.type.MobBehaviors.flying;
import static org.allaymc.server.entity.type.MobBehaviors.ground;
import static org.allaymc.server.entity.type.MobBehaviors.hasValidTarget;
import static org.allaymc.server.entity.type.MobBehaviors.roam;
import static org.allaymc.server.entity.type.MobBehaviors.spaceRoam;
import static org.allaymc.server.entity.type.MobBehaviors.targeting;

@SuppressWarnings("unused")
@UtilityClass
public final class HostileMobEntityTypeInitializer {

    private static final float ZOMBIE_SPEED = 0.1f;
    private static final float SKELETON_SPEED = 0.12f;
    private static final float SPIDER_SPEED = 0.2f;
    private static final float ROAM_SPEED = 0.1f;
    private static final double MELEE_RANGE = Math.sqrt(2.5);

    public static void initHusk() {
        EntityTypes.HUSK = AllayEntityType
                .builder(EntityHuskImpl.class)
                .vanillaEntity(EntityId.HUSK)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.6, 1.9), EntityMobBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::sunproofUndead, EntityUndeadComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(20, HostileMobEntityTypeInitializer::zombieLoot, HostileMobEntityTypeInitializer::zombieXp),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> MobBehaviors.hostile(40, ROAM_SPEED, (memory, clear) -> new MobMeleeAttackExecutor(
                        memory, ZOMBIE_SPEED, 40, clear, 30, MELEE_RANGE, 3,
                        MobMeleeAttackExecutor.effect(EffectTypes.HUNGER, 0, 30 * 20))), EntityAIComponentImpl.class)
                .build();
    }

    public static void initZombieVillagers() {
        EntityTypes.ZOMBIE_VILLAGER_V2 = zombieVillager(EntityZombieVillagerV2Impl.class, EntityId.ZOMBIE_VILLAGER_V2);
        EntityTypes.ZOMBIE_VILLAGER = zombieVillager(EntityZombieVillagerImpl.class, EntityId.ZOMBIE_VILLAGER);
    }

    private static <T extends Entity> EntityType<T> zombieVillager(Class<? extends EntityImpl> implClass, EntityId id) {
        return AllayEntityType
                .builder(implClass)
                .vanillaEntity(id)
                .addComponent(initInfo -> new EntityZombieVillagerBaseComponentImpl(initInfo, () -> EntityTypes.VILLAGER_V2),
                        EntityZombieVillagerBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(20, HostileMobEntityTypeInitializer::zombieLoot, HostileMobEntityTypeInitializer::zombieXp)
                                .immuneTo(DamageType.DROWN),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> MobBehaviors.hostile(40, ROAM_SPEED, (memory, clear) -> new MobMeleeAttackExecutor(
                        memory, ZOMBIE_SPEED, 40, clear, 30, MELEE_RANGE, 3, null)), EntityAIComponentImpl.class)
                .build();
    }

    public static void initDrowned() {
        EntityTypes.DROWNED = AllayEntityType
                .builder(EntityDrownedImpl.class)
                .vanillaEntity(EntityId.DROWNED)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.6, 1.9)
                                .weapon(() -> {
                                    var roll = ThreadLocalRandom.current().nextInt(10000);
                                    return roll < 85 ? ItemTypes.FISHING_ROD : roll < 1585 ? ItemTypes.TRIDENT : null;
                                })
                                .offhand(() -> ThreadLocalRandom.current().nextInt(100) < 3 ? ItemTypes.NAUTILUS_SHELL : null),
                        EntityMobBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(20, HostileMobEntityTypeInitializer::drownedLoot, HostileMobEntityTypeInitializer::zombieXp)
                                .breathing(EntityMobLivingComponentImpl.Breathing.AMPHIBIOUS),
                        EntityMobLivingComponentImpl.class)
                .addComponent(() -> new EntityHumanPhysicsComponentImpl() {
                    @Override
                    public double getWaterBuoyancy() {
                        return 0;
                    }
                }, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::drownedBehavior, EntityAIComponentImpl.class)
                .build();
    }

    private static EntityAIComponentImpl drownedBehavior() {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(40, 0, 20))
                .behavior(behavior(drownedTrident(MemoryTypes.ATTACK_TARGET, true),
                        targeting(MemoryTypes.ATTACK_TARGET, HostileMobEntityTypeInitializer::holdsTrident), 3))
                .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, ZOMBIE_SPEED, 40, true, 30, MELEE_RANGE, 3, null),
                        targeting(MemoryTypes.ATTACK_TARGET, entity -> !holdsTrident(entity)), 3))
                .behavior(behavior(drownedTrident(MemoryTypes.NEAREST_PLAYER, false),
                        targeting(MemoryTypes.NEAREST_PLAYER, HostileMobEntityTypeInitializer::holdsTrident,
                                HostileMobEntityTypeInitializer::drownedWantsToHunt), 2))
                .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, ZOMBIE_SPEED, 40, false, 30, MELEE_RANGE, 3, null),
                        targeting(MemoryTypes.NEAREST_PLAYER, entity -> !holdsTrident(entity),
                                HostileMobEntityTypeInitializer::drownedWantsToHunt), 2))
                .behavior(roam(ROAM_SPEED, 12));
        return new EntityAIComponentImpl(ground(builder).build());
    }

    static ProjectileAttackExecutor drownedTrident(MemoryType<Long> memory, boolean clear) {
        return new ProjectileAttackExecutor(memory, ZOMBIE_SPEED, 32, 10, 3, clear, 40,
                () -> EntityTypes.THROWN_TRIDENT, 1.6, SoundNames.MOB_DROWNED_SHOOT)
                .arc(0.1)
                .configure(projectile -> ((EntityThrownTrident) projectile).setTridentItem(null));
    }

    private static boolean holdsTrident(EntityIntelligent entity) {
        return entity instanceof EntityContainerHolderComponent holder
               && holder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType() == ItemTypes.TRIDENT;
    }

    private static boolean drownedWantsToHunt(EntityIntelligent entity) {
        var time = entity.getWorld().getWorldData().getTimeOfDay() % 24000;
        if (time >= 13000 && time < 23000) {
            return true;
        }

        var targetId = entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        return target != null && target.isTouchingWater();
    }

    public static void initSkeletonVariants() {
        EntityTypes.STRAY = AllayEntityType
                .builder(EntityStrayImpl.class)
                .vanillaEntity(EntityId.STRAY)
                .addComponent(initInfo -> new EntityArmedBaseComponentImpl(initInfo, () -> ItemTypes.BOW, 0.6, 1.9),
                        EntityArmedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(20,
                        (context, drops) -> skeletonLoot(context, drops, PotionType.SLOWNESS, 1, true), MobLoot.HOSTILE_XP),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> tippedArcher(PotionType.SLOWNESS), EntityAIComponentImpl.class)
                .build();

        EntityTypes.BOGGED = AllayEntityType
                .builder(EntityBoggedImpl.class)
                .vanillaEntity(EntityId.BOGGED)
                .addComponent(EntityBoggedBaseComponentImpl::new, EntityBoggedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(16,
                        (context, drops) -> skeletonLoot(context, drops, PotionType.POISON, 1, false), MobLoot.HOSTILE_XP),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> tippedArcher(PotionType.POISON), EntityAIComponentImpl.class)
                .build();

        EntityTypes.PARCHED = AllayEntityType
                .builder(EntityParchedImpl.class)
                .vanillaEntity(EntityId.PARCHED)
                .addComponent(initInfo -> new EntityArmedBaseComponentImpl(initInfo, () -> ItemTypes.BOW, 0.6, 1.9),
                        EntityArmedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(16,
                        (context, drops) -> skeletonLoot(context, drops, PotionType.WEAKNESS, 2, true), MobLoot.HOSTILE_XP),
                        EntityMobLivingComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::sunproofUndead, EntityUndeadComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> tippedArcher(PotionType.WEAKNESS), EntityAIComponentImpl.class)
                .build();
    }

    private static EntityAIComponentImpl tippedArcher(PotionType potionType) {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(16, 0, 20))
                .behavior(behavior(new TippedBowAttackExecutor(MemoryTypes.ATTACK_TARGET, SKELETON_SPEED, 40, true, potionType),
                        targeting(MemoryTypes.ATTACK_TARGET), 3))
                .behavior(behavior(new TippedBowAttackExecutor(MemoryTypes.NEAREST_PLAYER, SKELETON_SPEED, 40, false, potionType),
                        targeting(MemoryTypes.NEAREST_PLAYER), 2))
                .behavior(roam(ROAM_SPEED, 12));
        return new EntityAIComponentImpl(ground(builder).build());
    }

    public static void initCaveSpider() {
        EntityTypes.CAVE_SPIDER = AllayEntityType
                .builder(EntityCaveSpiderImpl.class)
                .vanillaEntity(EntityId.CAVE_SPIDER)
                .addComponent(EntityCaveSpiderBaseComponentImpl::new, EntityCaveSpiderBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(12, HostileMobEntityTypeInitializer::spiderLoot, MobLoot.HOSTILE_XP),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntitySpiderPhysicsComponentImpl::new, EntitySpiderPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> MobBehaviors.hostile(16, ROAM_SPEED, (memory, clear) -> new MobMeleeAttackExecutor(
                        memory, SPIDER_SPEED, 32, clear, 20, 1.6, 2, HostileMobEntityTypeInitializer::caveSpiderPoison)),
                        EntityAIComponentImpl.class)
                .build();
    }

    private static void caveSpiderPoison(EntityIntelligent attacker, EntityLiving target) {
        var seconds = switch (attacker.getWorld().getWorldData().getDifficulty()) {
            case PEACEFUL, EASY -> 0;
            case NORMAL -> 7;
            case HARD -> 15;
        };
        if (seconds > 0) {
            target.addEffect(new EffectInstance(EffectTypes.POISON, 0, seconds * 20, false, true));
        }
    }

    public static void initPiglinBrute() {
        EntityTypes.PIGLIN_BRUTE = AllayEntityType
                .builder(EntityPiglinBruteImpl.class)
                .vanillaEntity(EntityId.PIGLIN_BRUTE)
                .addComponent(initInfo -> new EntityArmedBaseComponentImpl(initInfo, () -> ItemTypes.GOLDEN_AXE, 0.6, 1.9),
                        EntityArmedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(50, MobLoot.NONE, MobLoot.xp(20)), EntityMobLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> MobBehaviors.hostile(12, ROAM_SPEED, (memory, clear) -> new MobMeleeAttackExecutor(
                        memory, 0.16f, 32, clear, 20, MELEE_RANGE, 7, null)), EntityAIComponentImpl.class)
                .build();
    }

    public static void initZoglin() {
        EntityTypes.ZOGLIN = AllayEntityType
                .builder(EntityZoglinImpl.class)
                .vanillaEntity(EntityId.ZOGLIN)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.4, 1.4), EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(40, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.ROTTEN_FLESH, context.count(1, 3)), MobLoot.HOSTILE_XP).fireproof(),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::sunproofUndead, EntityUndeadComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> MobBehaviors.hostile(16, ROAM_SPEED, (memory, clear) -> new MobMeleeAttackExecutor(
                        memory, 0.13f, 32, clear, 40, 2.2, 3, 8, null).knockback(0.5, 0.6)), EntityAIComponentImpl.class)
                .build();
    }

    public static void initRavager() {
        EntityTypes.RAVAGER = AllayEntityType
                .builder(EntityRavagerImpl.class)
                .vanillaEntity(EntityId.RAVAGER)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 1.95, 2.2), EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(100, (context, drops) -> {
                    if (context.killedByPlayer()) {
                        MobLoot.add(drops, ItemTypes.SADDLE, 1);
                    }
                }, MobLoot.xp(20)), EntityMobLivingComponentImpl.class)
                .addComponent(() -> heavyPhysics(0.75f), EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> MobBehaviors.hostile(16, 0.08f, (memory, clear) -> new MobMeleeAttackExecutor(
                        memory, 0.18f, 32, clear, 15, 2.8, 12, null).knockback(1.0, 0.5)), EntityAIComponentImpl.class)
                .build();
    }

    public static void initVex() {
        EntityTypes.VEX = AllayEntityType
                .builder(EntityVexImpl.class)
                .vanillaEntity(EntityId.VEX)
                .addComponent(initInfo -> new EntityVexBaseComponentImpl(initInfo).weapon(() -> ItemTypes.IRON_SWORD),
                        EntityVexBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(14, MobLoot.NONE, MobLoot.HOSTILE_XP)
                        .fireproof().immuneTo(DamageType.FALL), EntityMobLivingComponentImpl.class)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::vexBehavior, EntityAIComponentImpl.class)
                .build();
    }

    private static EntityAIComponentImpl vexBehavior() {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(32, 0, 20))
                .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.25f, 48, true, 20, 1.5, 3, null),
                        targeting(MemoryTypes.ATTACK_TARGET), 4))
                .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.25f, 48, false, 20, 1.5, 3, null),
                        targeting(MemoryTypes.NEAREST_PLAYER), 3))
                .behavior(behavior(new FollowEntityExecutor(EntityVexBaseComponentImpl.OWNER, 0.2f, 48 * 48, 9),
                        HostileMobEntityTypeInitializer::vexStrayedFromOwner, 2))
                .behavior(spaceRoam(0.1f, 8, 4, 60, new FlyingPosEvaluator()));
        return new EntityAIComponentImpl(flying(builder).build());
    }

    private static boolean vexStrayedFromOwner(EntityIntelligent entity) {
        var ownerId = entity.getMemoryStorage().get(EntityVexBaseComponentImpl.OWNER);
        var owner = ownerId == null ? null : entity.getDimension().getEntityManager().getEntity(ownerId);
        return owner != null && owner.isAlive() && entity.getLocation().distanceSquared(owner.getLocation()) > 56.25;
    }

    public static void initGhast() {
        EntityTypes.GHAST = AllayEntityType
                .builder(EntityGhastImpl.class)
                .vanillaEntity(EntityId.GHAST)
                .addComponent(EntityGhastBaseComponentImpl::new, EntityGhastBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(10, HostileMobEntityTypeInitializer::ghastLoot, MobLoot.HOSTILE_XP)
                        .fireproof().immuneTo(DamageType.FALL), EntityMobLivingComponentImpl.class)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(28, 0, 20))
                            .behavior(behavior(new GhastFireballAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.08f, 64, 20, 8, true, 10, 50, 64),
                                    targeting(MemoryTypes.ATTACK_TARGET), 3))
                            .behavior(behavior(new GhastFireballAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.08f, 64, 20, 8, false, 10, 50, 64),
                                    targeting(MemoryTypes.NEAREST_PLAYER), 2))
                            .behavior(spaceRoam(0.06f, 16, 6, 100, new FlyingPosEvaluator()));
                    return new EntityAIComponentImpl(flying(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initBreeze() {
        EntityTypes.BREEZE = AllayEntityType
                .builder(EntityBreezeImpl.class)
                .vanillaEntity(EntityId.BREEZE)
                .addComponent(initInfo -> new EntityMobBaseComponentImpl(initInfo, 0.6, 1.77), EntityMobBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(30, (context, drops) -> {
                    if (context.killedByPlayer()) {
                        MobLoot.add(drops, ItemTypes.BREEZE_ROD, context.count(1, 2));
                    }
                }, MobLoot.xp(10)).immuneTo(DamageType.FALL).damageFilter(damage ->
                        damage.getDamageType() != DamageType.PROJECTILE
                        || damage.getAttacker() instanceof EntityWindChargeProjectile
                        || damage.getAttacker() instanceof EntityBreezeWindChargeProjectile), EntityMobLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(24, 0, 20))
                            .coreBehavior(behavior(new BreezeJumpExecutor(), all(
                                    EntityIntelligent::isOnGround,
                                    entity -> hasValidTarget(entity, MemoryTypes.ATTACK_TARGET) || hasValidTarget(entity, MemoryTypes.NEAREST_PLAYER)
                            ), 1, 50))
                            .behavior(behavior(breezeShot(MemoryTypes.ATTACK_TARGET, true), targeting(MemoryTypes.ATTACK_TARGET), 3))
                            .behavior(behavior(breezeShot(MemoryTypes.NEAREST_PLAYER, false), targeting(MemoryTypes.NEAREST_PLAYER), 2))
                            .behavior(roam(0.12f, 12));
                    return new EntityAIComponentImpl(ground(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    private static ProjectileAttackExecutor breezeShot(MemoryType<Long> memory, boolean clear) {
        return new ProjectileAttackExecutor(memory, 0.18f, 24, 12, 4, clear, 30,
                () -> EntityTypes.BREEZE_WIND_CHARGE_PROJECTILE, 0.7, SoundNames.MOB_BREEZE_SHOOT).arc(0);
    }

    public static void initGuardians() {
        EntityTypes.GUARDIAN = guardian(EntityGuardianImpl.class, EntityId.GUARDIAN, false);
        EntityTypes.ELDER_GUARDIAN = guardian(EntityElderGuardianImpl.class, EntityId.ELDER_GUARDIAN, true);
    }

    private static <T extends Entity> EntityType<T> guardian(Class<? extends EntityImpl> implClass, EntityId id, boolean elder) {
        return AllayEntityType
                .builder(implClass)
                .vanillaEntity(id)
                .addComponent(initInfo -> new EntityGuardianBaseComponentImpl(initInfo, elder), EntityGuardianBaseComponentImpl.class)
                .addComponent(() -> new EntityGuardianLivingComponentImpl(elder ? 80 : 30,
                        (context, drops) -> guardianLoot(context, drops, elder), MobLoot.xp(10)), EntityGuardianLivingComponentImpl.class)
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var evaluator = new SwimmingPosEvaluator();
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            .behavior(behavior(new GuardianLaserExecutor(MemoryTypes.ATTACK_TARGET, 15, true, 60, 40, 5),
                                    targeting(MemoryTypes.ATTACK_TARGET), 3))
                            .behavior(behavior(new GuardianLaserExecutor(MemoryTypes.NEAREST_PLAYER, 15, false, 60, 40, 5),
                                    targeting(MemoryTypes.NEAREST_PLAYER), 2))
                            .behavior(spaceRoam(0.12f, 12, 4, 80, evaluator));
                    return new EntityAIComponentImpl(MobBehaviors.swimming(builder, evaluator).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initSnowGolem() {
        EntityTypes.SNOW_GOLEM = AllayEntityType
                .builder(EntitySnowGolemImpl.class)
                .vanillaEntity(EntityId.SNOW_GOLEM)
                .addComponent(EntitySnowGolemBaseComponentImpl::new, EntitySnowGolemBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(4, (context, drops) ->
                        MobLoot.add(drops, ItemTypes.SNOWBALL, context.random().nextInt(16)), MobLoot.NO_XP)
                        .immuneTo(DamageType.FALL), EntityMobLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestMonsterSensor(10, 10))
                            .behavior(behavior(new ProjectileAttackExecutor(NearestMonsterSensor.NEAREST_MONSTER, 0.09f, 16, 10, 0, true, 20,
                                            () -> EntityTypes.SNOWBALL, 1.5, SoundNames.MOB_SNOWGOLEM_SHOOT),
                                    new MemoryCheckNotEmptyEvaluator(NearestMonsterSensor.NEAREST_MONSTER), 2))
                            .behavior(roam(0.08f, 10));
                    return new EntityAIComponentImpl(ground(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initCreaking() {
        EntityTypes.CREAKING = AllayEntityType
                .builder(EntityCreakingImpl.class)
                .vanillaEntity(EntityId.CREAKING)
                .setProperties(EntityPropertyTypes.CREAKING_STATE, EntityPropertyTypes.CREAKING_SWAYING_TICKS)
                .addComponent(EntityCreakingBaseComponentImpl::new, EntityCreakingBaseComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::creakingLiving, EntityMobLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(24, 0, 20))
                            .behavior(behavior(entity -> {
                                EntityControlHelper.removeRouteTarget(entity);
                                return isObservedCreaking(entity);
                            }, HostileMobEntityTypeInitializer::isObservedCreaking, 5))
                            .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.18f, 32, true, 40, 1.8, 3, null),
                                    targeting(MemoryTypes.ATTACK_TARGET), 3))
                            .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.18f, 32, false, 40, 1.8, 3, null),
                                    targeting(MemoryTypes.NEAREST_PLAYER), 2))
                            .behavior(roam(0.08f, 12));
                    return new EntityAIComponentImpl(ground(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    private static boolean isObservedCreaking(EntityIntelligent entity) {
        return entity instanceof EntityImpl impl && impl.getBaseComponent() instanceof EntityCreakingBaseComponentImpl creaking
               && creaking.isObserved();
    }

    /**
     * Creakings keep 1 health so destroying the heart can still remove them, but a landed hit
     * only plays the sway. Void, commands, and the plugin API remain lethal.
     */
    private static EntityMobLivingComponentImpl creakingLiving() {
        return new EntityMobLivingComponentImpl(1, MobLoot.NONE, MobLoot.NO_XP) {
            @Override
            public boolean canBeAttacked(DamageContainer damage) {
                return !creakingShrugsOff(damage) && super.canBeAttacked(damage);
            }

            @Override
            public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
                if (thisEntity.isAlive() && creakingShrugsOff(damage)) {
                    if (damage.getAttacker() != null
                            && thisEntity instanceof EntityImpl impl
                            && impl.getBaseComponent() instanceof EntityCreakingBaseComponentImpl creaking) {
                        creaking.startSwaying();
                    }
                    return false;
                }
                return super.attack(damage, ignoreCoolDown);
            }
        };
    }

    private static boolean creakingShrugsOff(DamageContainer damage) {
        var type = damage.getDamageType();
        return type != DamageType.VOID && type != DamageType.COMMAND && type != DamageType.API;
    }

    public static void initWarden() {
        EntityTypes.WARDEN = AllayEntityType
                .builder(EntityWardenImpl.class)
                .vanillaEntity(EntityId.WARDEN)
                .addComponent(EntityWardenBaseComponentImpl::new, EntityWardenBaseComponentImpl.class)
                .addComponent(EntityWardenLivingComponentImpl::new, EntityWardenLivingComponentImpl.class)
                .addComponent(() -> heavyPhysics(1f), EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var builder = BehaviorGroupImpl.builder()
                            .behavior(behavior(new WardenSonicBoomExecutor(MemoryTypes.ATTACK_TARGET),
                                    entity -> WardenSonicBoomExecutor.canBoom(entity, MemoryTypes.ATTACK_TARGET), 4))
                            .behavior(behavior(new MobMeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.16f, 64, false, 18, 2.2, 30, null),
                                    targeting(MemoryTypes.ATTACK_TARGET), 3))
                            .behavior(roam(0.05f, 10));
                    return new EntityAIComponentImpl(ground(builder).build());
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initEvoker() {
        EntityTypes.EVOCATION_FANG = AllayEntityType
                .builder(EntityEvocationFangImpl.class)
                .vanillaEntity(EntityId.EVOCATION_FANG)
                .addComponent(EntityEvocationFangBaseComponentImpl::new, EntityEvocationFangBaseComponentImpl.class)
                .build();

        EntityTypes.EVOCATION_ILLAGER = AllayEntityType
                .builder(EntityEvocationIllagerImpl.class)
                .vanillaEntity(EntityId.EVOCATION_ILLAGER)
                .addComponent(EntityEvokerBaseComponentImpl::new, EntityEvokerBaseComponentImpl.class)
                .addComponent(() -> new EntityMobLivingComponentImpl(24, HostileMobEntityTypeInitializer::evokerLoot, context -> 10),
                        EntityMobLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(HostileMobEntityTypeInitializer::evokerBehavior, EntityAIComponentImpl.class)
                .build();
    }

    private static EntityAIComponentImpl evokerBehavior() {
        var builder = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(20, 0, 20))
                .behavior(behavior(new EvokerSpellExecutor(MemoryTypes.ATTACK_TARGET, EvokerSpellExecutor.Spell.SUMMON_VEX),
                        targeting(MemoryTypes.ATTACK_TARGET, entity -> canSummon(entity)), 6))
                .behavior(behavior(new EvokerSpellExecutor(MemoryTypes.NEAREST_PLAYER, EvokerSpellExecutor.Spell.SUMMON_VEX),
                        targeting(MemoryTypes.NEAREST_PLAYER, entity -> canSummon(entity) && !hasValidTarget(entity, MemoryTypes.ATTACK_TARGET)), 5))
                .behavior(behavior(new EvokerSpellExecutor(MemoryTypes.ATTACK_TARGET, EvokerSpellExecutor.Spell.FANGS),
                        targeting(MemoryTypes.ATTACK_TARGET, entity -> canCastFangs(entity)), 4))
                .behavior(behavior(new EvokerSpellExecutor(MemoryTypes.NEAREST_PLAYER, EvokerSpellExecutor.Spell.FANGS),
                        targeting(MemoryTypes.NEAREST_PLAYER, entity -> canCastFangs(entity) && !hasValidTarget(entity, MemoryTypes.ATTACK_TARGET)), 3))
                .behavior(behavior(new FleeFromTargetExecutor(MemoryTypes.NEAREST_PLAYER, 0.15f, 4.5, 20),
                        targeting(MemoryTypes.NEAREST_PLAYER, HostileMobEntityTypeInitializer::playerTooClose), 2))
                .behavior(roam(0.08f, 12));
        return new EntityAIComponentImpl(ground(builder).build());
    }

    private static EntityEvokerBaseComponentImpl evoker(EntityIntelligent entity) {
        return (EntityEvokerBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
    }

    private static boolean canSummon(EntityIntelligent entity) {
        return entity.getTick() >= evoker(entity).getNextSummonTick()
               && EvokerSpellExecutor.countVexes(entity) < EvokerSpellExecutor.VEX_CAP;
    }

    private static boolean canCastFangs(EntityIntelligent entity) {
        return entity.getTick() >= evoker(entity).getNextFangCastTick();
    }

    private static boolean playerTooClose(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        return target != null && entity.getLocation().distanceSquared(target.getLocation()) < 64;
    }

    private static EntityUndeadComponentImpl sunproofUndead() {
        return new EntityUndeadComponentImpl() {
            @Override
            public boolean ignitedBySunlight() {
                return false;
            }
        };
    }

    private static EntityMobPhysicsComponentImpl heavyPhysics(float knockbackResistance) {
        var physics = new EntityMobPhysicsComponentImpl();
        physics.setKnockbackResistance(knockbackResistance);
        return physics;
    }

    private static int zombieXp(LootContext context) {
        if (!context.killedByPlayer()) {
            return 0;
        }
        return context.baby() ? 12 : 5;
    }

    private static void zombieLoot(LootContext context, List<ItemStack> drops) {
        MobLoot.add(drops, ItemTypes.ROTTEN_FLESH, context.count(0, 2));
        if (context.killedByPlayer() && context.chance(0.025, 0.01)) {
            var rare = switch (context.random().nextInt(3)) {
                case 0 -> ItemTypes.IRON_INGOT;
                case 1 -> ItemTypes.CARROT;
                default -> ItemTypes.POTATO;
            };
            MobLoot.add(drops, rare, 1);
        }
    }

    private static void drownedLoot(LootContext context, List<ItemStack> drops) {
        MobLoot.add(drops, ItemTypes.ROTTEN_FLESH, context.count(0, 2));
        if (context.killedByPlayer() && context.chance(0.11, 0.02)) {
            MobLoot.add(drops, ItemTypes.COPPER_INGOT, 1);
        }
        if (context.entity() instanceof EntityContainerHolderComponent holder
                && holder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType() == ItemTypes.TRIDENT
                && context.chance(0.085, 0.01)) {
            MobLoot.add(drops, ItemTypes.TRIDENT, 1);
        }
    }

    private static void skeletonLoot(LootContext context, List<ItemStack> drops, PotionType arrowPotion,
                                     int maxTipped, boolean playerOnly) {
        MobLoot.add(drops, ItemTypes.ARROW, context.count(0, 2));
        MobLoot.add(drops, ItemTypes.BONE, context.count(0, 2));
        if (playerOnly && !context.killedByPlayer()) {
            return;
        }

        var tipped = Math.min(context.random().nextInt(maxTipped + 1) + context.random().nextInt(context.looting() + 1), maxTipped + 1);
        if (tipped > 0) {
            var arrows = ItemTypes.ARROW.createItemStack(tipped);
            arrows.setPotionType(arrowPotion);
            drops.add(arrows);
        }
    }

    private static void spiderLoot(LootContext context, List<ItemStack> drops) {
        MobLoot.add(drops, ItemTypes.STRING, context.count(0, 2));
        if (context.killedByPlayer()) {
            MobLoot.add(drops, ItemTypes.SPIDER_EYE, context.count(0, 1));
        }
    }

    private static void ghastLoot(LootContext context, List<ItemStack> drops) {
        MobLoot.add(drops, ItemTypes.GHAST_TEAR, context.count(0, 1));
        MobLoot.add(drops, ItemTypes.GUNPOWDER, context.count(0, 2));
    }

    private static void guardianLoot(LootContext context, List<ItemStack> drops, boolean elder) {
        MobLoot.add(drops, ItemTypes.PRISMARINE_SHARD, context.count(0, 2));
        var roll = context.random().nextInt(elder ? 6 : 5);
        if (roll < (elder ? 3 : 2)) {
            MobLoot.add(drops, ItemTypes.COD, 1 + context.random().nextInt(context.looting() + 1));
        } else if (roll < (elder ? 5 : 4)) {
            MobLoot.add(drops, ItemTypes.PRISMARINE_CRYSTALS, 1 + context.random().nextInt(context.looting() + 1));
        }
        if (context.killedByPlayer() && elder) {
            MobLoot.add(drops, ItemTypes.WET_SPONGE, 1);
        }
        if (context.killedByPlayer() && context.chance(0.025, 0.01)) {
            MobLoot.add(drops, ItemTypes.COD, 1);
        }
        if (elder && context.random().nextInt(5) == 0) {
            MobLoot.add(drops, ItemTypes.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
        }
    }

    private static void evokerLoot(LootContext context, List<ItemStack> drops) {
        MobLoot.add(drops, ItemTypes.EMERALD, context.count(0, 1));
        MobLoot.add(drops, ItemTypes.TOTEM_OF_UNDYING, 1);
    }
}
