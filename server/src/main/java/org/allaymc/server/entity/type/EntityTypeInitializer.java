package org.allaymc.server.entity.type;

import lombok.experimental.UtilityClass;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.interfaces.*;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.data.ItemTags;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.utils.DyeColor;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.entity.ai.behavior.BehaviorImpl;
import org.allaymc.server.entity.ai.behaviorgroup.BehaviorGroupImpl;
import org.allaymc.server.entity.ai.controller.FluctuateController;
import org.allaymc.server.entity.ai.controller.FlyController;
import org.allaymc.server.entity.ai.controller.CubeJumpController;
import org.allaymc.server.entity.ai.controller.SwimController;
import org.allaymc.server.entity.ai.controller.LookController;
import org.allaymc.server.entity.ai.controller.WalkController;
import org.allaymc.server.entity.ai.evaluator.BlockCheckEvaluator;
import org.allaymc.server.entity.ai.evaluator.MemoryCheckNotEmptyEvaluator;
import org.allaymc.server.entity.ai.evaluator.PassByTimeEvaluator;
import org.allaymc.server.entity.ai.evaluator.ProbabilityEvaluator;
import org.allaymc.server.entity.ai.executor.*;
import org.allaymc.server.entity.ai.route.finder.FlatAStarRouteFinder;
import org.allaymc.server.entity.ai.route.finder.SpaceAStarRouteFinder;
import org.allaymc.server.entity.ai.route.posevaluator.FlyingPosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.SwimmingPosEvaluator;
import org.allaymc.server.entity.ai.route.posevaluator.WalkingPosEvaluator;
import org.allaymc.server.entity.ai.sensor.NearestFeedingPlayerSensor;
import org.allaymc.server.entity.ai.sensor.NearestFishSensor;
import org.allaymc.server.entity.ai.sensor.NearestAbsorbableSensor;
import org.allaymc.server.entity.ai.sensor.NearestPlayerSensor;
import org.allaymc.server.entity.component.*;
import org.allaymc.server.entity.component.animal.*;
import org.allaymc.server.entity.component.aquatic.*;
import org.allaymc.server.entity.component.cube.*;
import org.allaymc.server.entity.component.sulfurcube.*;
import org.allaymc.server.entity.component.humanlike.EntityArmedBaseComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityHumanLikeBaseComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityHumanLikeContainerHolderComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityHumanPhysicsComponentImpl;
import org.allaymc.server.entity.component.humanlike.EntityPiglinBaseComponentImpl;
import org.allaymc.server.entity.component.item.*;
import org.allaymc.server.entity.component.player.EntityPlayerBaseComponentImpl;
import org.allaymc.server.entity.component.player.EntityPlayerContainerHolderComponentImpl;
import org.allaymc.server.entity.component.player.EntityPlayerLivingComponentImpl;
import org.allaymc.server.entity.component.player.EntityPlayerPhysicsComponentImpl;
import org.allaymc.server.entity.component.projectile.*;
import org.allaymc.server.entity.data.EntityId;
import org.allaymc.server.entity.impl.*;
import org.joml.Vector3i;

import java.util.concurrent.ThreadLocalRandom;

import static org.allaymc.server.entity.ai.evaluator.LogicHelper.all;
import static org.allaymc.server.entity.ai.evaluator.LogicHelper.any;

/**
 * @author daoge_cmd
 */
@SuppressWarnings("unused")
@UtilityClass
public final class EntityTypeInitializer {

    /**
     * Kovalama hizlari; zombinin {@code 0.1f} taban degeriyle ayni birimde. Kurt ve endermite
     * zombiden belirgin sekilde hizli, enderman ve piglin bir tik hizli.
     */
    private static final float WOLF_SPEED = 0.3f;
    private static final float ENDERMITE_SPEED = 0.28f;
    private static final float ENDERMAN_SPEED = 0.15f;
    private static final float PIGLIN_SPEED = 0.13f;
    private static final float BLAZE_SPEED = 0.14f;

    private static final float FOX_BASE_SPEED = 0.3f;
    private static final float FOX_PANIC_SPEED = FOX_BASE_SPEED * 1.25f;
    private static final float FOX_TEMPT_SPEED = FOX_BASE_SPEED * 0.5f;
    private static final float FOX_STROLL_SPEED = FOX_BASE_SPEED * 0.8f;

    /**
     * Blaze'in ates topu ritmi, vanilla'dan alindi: yaklasik bir saniye sarj eder, birkac tick
     * arayla uc ates topu savurur, sonra kabaca bes saniye susar. Ayrica burun buruna dovusmeyi
     * reddeder; {@link #BLAZE_MIN_RANGE} degerinden yakina girilirse geri suzulur.
     */
    private static final double BLAZE_PREFERRED_RANGE = 12;
    private static final double BLAZE_MIN_RANGE = 5;
    private static final int BLAZE_CHARGE_TIME = 20;
    private static final int BLAZE_COOLDOWN = 100;
    private static final int BLAZE_BURST_SIZE = 3;

    private static final float SKELETON_SPEED = 0.12f;
    private static final double SKELETON_PREFERRED_RANGE = 10;
    private static final double SKELETON_MIN_RANGE = 4;
    private static final int SKELETON_COOLDOWN = 40;

    private static final float PILLAGER_SPEED = 0.13f;
    private static final double PILLAGER_PREFERRED_RANGE = 12;
    private static final double PILLAGER_MIN_RANGE = 5;
    private static final int PILLAGER_COOLDOWN = 60;

    private static final float VINDICATOR_SPEED = 0.18f;

    private static final float WITCH_SPEED = 0.11f;
    private static final double WITCH_PREFERRED_RANGE = 8;
    private static final double WITCH_MIN_RANGE = 3;
    private static final int WITCH_COOLDOWN = 60;

    /**
     * Creeper zamanlamasi. Fitili yalnizca {@link #CREEPER_FUSE_RANGE} icinde yakar ve patlamasi
     * {@link #CREEPER_FUSE_TIME} tick surer; oyuncunun kacmak icin sahip oldugu sure budur.
     */
    /**
     * Sulfur kupunun sicrayis olculeri (buyuk kup icin; kucuk kup bunlarin dortte ucunu alir).
     *
     * <p>Dikey deger, motorun yercekimiyle birlikte yaklasik bir bloklik bir sicrayis veriyor —
     * yani kup zorlanarak tek blogu asiyor, ustunden atlamiyor. Once denenen {@code 0.52} bunun
     * bir buçuk katiydi ve kup gorunur sekilde fazla yukseliyordu; o deger bir evcil hayvan
     * uygulamasindan gelmisti ve sahibine yetisebilmek icin bilerek abartilmisti.</p>
     */
    private static final float SULFUR_CUBE_JUMP_SPEED = 0.24f;
    private static final float SULFUR_CUBE_JUMP_HEIGHT = 0.42f;
    private static final int SULFUR_CUBE_JUMP_INTERVAL = 10;
    private static final int SULFUR_CUBE_JUMP_ANIMATION_TICKS = 8;

    /** Sulfur kupunun taban hizi ve emilebilir blok arama menzili. */
    private static final float SULFUR_CUBE_SPEED = 0.12f;
    private static final double SULFUR_CUBE_SEEK_RANGE = 16;

    /** Kup moblarinin (balcik, magma kupu) kovalama hizi ve vurus araligi. */
    private static final float CUBE_SPEED = 0.12f;
    private static final int CUBE_ATTACK_COOLDOWN = 20;

    /** Balik ve akselot hizlari; su icinde kara moblarindan daha akici hareket ederler. */
    // Balik hizlari resmi davranis paketindeki tur basina "minecraft:movement" degerleri; dordunde
    // de "minecraft:underwater_movement" ayni sayiyi tekrarliyor, yani suda ve disarida ayni hiz.
    private static final float COD_SPEED = 0.1f;
    private static final float SALMON_SPEED = 0.12f;
    private static final float TROPICALFISH_SPEED = 0.12f;
    private static final float PUFFERFISH_SPEED = 0.13f;

    // Kacis mesafesi resmi "behavior.avoid_mob_type" bileseninin max_dist degeri. Somon digerlerinden
    // daha gec urkuyor.
    private static final double FISH_FLEE_RANGE = 6;
    private static final double SALMON_FLEE_RANGE = 3;

    /**
     * Akselotun suda kullandigi hiz.
     *
     * <p>Resmi tanimda karada {@code 0.1}, suda {@code 0.2}. Akselotun buradaki gezinme mantigi
     * tumuyle su icinde calistigi icin gecerli olan deger su altindaki.</p>
     */
    private static final float AXOLOTL_SPEED = 0.2f;
    private static final double AXOLOTL_HUNT_RANGE = 16;

    /** Vanilla ari cani. */
    private static final int BEE_HEALTH = 10;

    /** Vanilla allay cani. */
    private static final int ALLAY_HEALTH = 20;
    /** Vanilla yarasa cani. */
    private static final int BAT_HEALTH = 6;
    /** Vanilla kurbaga cani. */
    private static final int FROG_HEALTH = 10;

    private static final float CREEPER_SPEED = 0.15f;
    private static final double CREEPER_FUSE_RANGE = 3;
    private static final int CREEPER_FUSE_TIME = 30;
    private static final float CREEPER_EXPLOSION_SIZE = 3;

    /**
     * Ari.
     *
     * <p>Motorda yalnizca varsayilan taban bilesenle kayitliydi: ne cani ne
     * fizigi vardi, yani vurulamiyor ve {@code setMotion} ile hareket
     * ettirilemiyordu. Burada canli varlik, ucus fizigi ve bas donusu
     * bilesenleri eklendi.</p>
     *
     * <p>Davranis grubu (yapay zeka) yok — ari kendi basina ucmaz, yerinde
     * asili durur. Ucus mantigini kendi kuran eklentiler icin dogru taban
     * budur; vanilla ari yapay zekasi eklenirse buraya girer.</p>
     */
    /**
     * Allay.
     *
     * <p>Ari ile ayni durumdaydi: yalnizca varsayilan taban bilesenle
     * kayitliydi, dolayisiyla cani yoktu ve <b>vurulamiyordu</b>. Oyuncunun
     * vurusu {@code ITEM_USE_ON_ENTITY_ATTACK} isleminde hedef
     * {@code EntityLivingComponent} degilse sessizce dusuyor; NPC olarak
     * kullanilan bir allay'a vurunca hicbir sey olmazdi.</p>
     *
     * <p>Davranis grubu (yapay zeka) yok — allay kendi basina ucmaz, yerinde
     * asili durur.</p>
     */
    public static void initAllay() {
        EntityTypes.ALLAY = AllayEntityType
                .builder(EntityAllayImpl.class)
                .vanillaEntity(EntityId.ALLAY)
                .addComponent(EntityAllayBaseComponentImpl::new, EntityAllayBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean hasFallDamage() {
                            // Ucan mob: dusme hasari almaz.
                            return false;
                        }
                    };
                    component.setMaxHealth(ALLAY_HEALTH);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .build();
    }

    /**
     * Yarasa.
     *
     * <p>Gerekce {@link #initAllay()} ile ayni: canli varlik bileseni olmadan
     * vurulamiyordu. Ucan mob oldugu icin dusme hasari almaz.</p>
     */
    public static void initBat() {
        EntityTypes.BAT = AllayEntityType
                .builder(EntityBatImpl.class)
                .vanillaEntity(EntityId.BAT)
                .addComponent(EntityBatBaseComponentImpl::new, EntityBatBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean hasFallDamage() {
                            // Ucan mob: dusme hasari almaz.
                            return false;
                        }
                    };
                    component.setMaxHealth(BAT_HEALTH);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .build();
    }

    /**
     * Kurbaga.
     *
     * <p>Gerekce {@link #initAllay()} ile ayni. Kara mobu oldugu icin ucus
     * degil normal mob fizigi kullanir ve dusme hasari alir.</p>
     */
    public static void initFrog() {
        EntityTypes.FROG = AllayEntityType
                .builder(EntityFrogImpl.class)
                .vanillaEntity(EntityId.FROG)
                .addComponent(EntityFrogBaseComponentImpl::new, EntityFrogBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl();
                    component.setMaxHealth(FROG_HEALTH);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .build();
    }

    public static void initBee() {
        EntityTypes.BEE = AllayEntityType
                .builder(EntityBeeImpl.class)
                .vanillaEntity(EntityId.BEE)
                .addComponent(EntityBeeBaseComponentImpl::new, EntityBeeBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean hasFallDamage() {
                            // Ucan mob: dusme hasari almaz.
                            return false;
                        }
                    };
                    component.setMaxHealth(BEE_HEALTH);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .build();
    }

    public static void initFallingBlock() {
        EntityTypes.FALLING_BLOCK = AllayEntityType
                .builder(EntityFallingBlockImpl.class)
                .vanillaEntity(EntityId.FALLING_BLOCK)
                .addComponent(EntityFallingBlockBaseComponentImpl::new, EntityFallingBlockBaseComponentImpl.class)
                .addComponent(() -> new EntityPhysicsComponentImpl() {
                    {
                        // The initial onGround state for falling block is false
                        // And it will be either turned into block or item based
                        // on the block which the falling block fell on
                        this.onGround = false;
                    }

                    @Override
                    public double getGravity() {
                        return 0.04;
                    }

                    @Override
                    public boolean computeLiquidPhysics() {
                        // Falling blocks have no special water physics in vanilla
                        return false;
                    }
                }, EntityPhysicsComponentImpl.class)
                .build();
    }

    public static void initItem() {
        EntityTypes.ITEM = AllayEntityType
                .builder(EntityItemImpl.class)
                .vanillaEntity(EntityId.ITEM)
                .addComponent(EntityItemBaseComponentImpl::new, EntityItemBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean canBeAttacked(DamageContainer damage) {
                            if (!super.canBeAttacked(damage)) {
                                return false;
                            }

                            var damageType = damage.getDamageType();
                            if (damageType == DamageType.FALLING_BLOCK ||
                                damageType == DamageType.STALACTITE) {
                                return false;
                            }

                            return true;
                        }

                        @Override
                        public boolean hasFallDamage() {
                            return false;
                        }

                        @Override
                        public boolean hasDrowningDamage() {
                            return false;
                        }

                        @Override
                        public boolean isFireproof() {
                            if (super.isFireproof()) {
                                return true;
                            }

                            var itemStack = ((EntityItem) thisEntity).getItemStack();
                            if (itemStack == null) {
                                return false;
                            }

                            var itemType = itemStack.getItemType();
                            return itemType.hasItemTag(ItemTags.FIREPROOF);
                        }

                        @Override
                        protected boolean hasDeadTimer() {
                            return false;
                        }
                    };
                    component.setMaxHealth(5);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(() -> new EntityPhysicsComponentImpl() {
                    @Override
                    public double getGravity() {
                        return 0.04;
                    }

                    @Override
                    public double getWaterBuoyancy() {
                        return 0.0405;
                    }

                    @Override
                    public double getWaterDragFactor() {
                        return 0.02;
                    }

                    @Override
                    public double getLavaBuoyancy() {
                        return 0.0405;
                    }

                    @Override
                    public double getLavaDragFactor() {
                        return 0.05;
                    }
                }, EntityPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initPlayer() {
        EntityTypes.PLAYER = AllayEntityType
                .builder(EntityPlayerImpl.class)
                .vanillaEntity(EntityId.PLAYER)
                .addComponent(EntityPlayerBaseComponentImpl::new, EntityPlayerBaseComponentImpl.class)
                .addComponent(EntityPlayerContainerHolderComponentImpl::new, EntityPlayerContainerHolderComponentImpl.class)
                .addComponent(EntityPlayerLivingComponentImpl::new, EntityPlayerLivingComponentImpl.class)
                .addComponent(EntityPlayerPhysicsComponentImpl::new, EntityPlayerPhysicsComponentImpl.class)
                .addComponent(EntitySleepableComponentImpl::new, EntitySleepableComponentImpl.class)
                .build();
    }

    public static void initVillagerV2() {
        EntityTypes.VILLAGER_V2 = AllayEntityType
                .builder(EntityVillagerV2Impl.class)
                .vanillaEntity(EntityId.VILLAGER_V2)
                .addComponent(EntityLivingComponentImpl::new, EntityLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .build();
    }

    public static void initZombie() {
        EntityTypes.ZOMBIE = AllayEntityType
                .builder(EntityZombieImpl.class)
                .vanillaEntity(EntityId.ZOMBIE)
                .addComponent(EntityHumanLikeBaseComponentImpl::new, EntityHumanLikeBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(EntityZombieLivingComponentImpl::new, EntityZombieLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(40, 0, 20))
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.1f, 40, true, 30))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, 0.1f, 40, 30))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initWolf() {
        EntityTypes.WOLF = AllayEntityType
                .builder(EntityWolfImpl.class)
                .vanillaEntity(EntityId.WOLF)
                .addComponent(EntityWolfBaseComponentImpl::new, EntityWolfBaseComponentImpl.class)
                .addComponent(EntityWolfLivingComponentImpl::new, EntityWolfLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            // Oncelik 3: bizi yaralayani kovala
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, WOLF_SPEED, 32, true, 20))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            // Oncelik 2: goz onundeki en yakin oyuncuya saldir
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, WOLF_SPEED, 32, 20))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            // Oncelik 1 (en dusuk): rastgele dolasma
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.12f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initEndermite() {
        EntityTypes.ENDERMITE = AllayEntityType
                .builder(EntityEndermiteImpl.class)
                .vanillaEntity(EntityId.ENDERMITE)
                .addComponent(EntityEndermiteBaseComponentImpl::new, EntityEndermiteBaseComponentImpl.class)
                .addComponent(EntityEndermiteLivingComponentImpl::new, EntityEndermiteLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            .behavior(BehaviorImpl.builder()
                                    // Kisa saldiri menzili: endermite minicik, tam dibine gelmesi gerekiyor.
                                    .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, ENDERMITE_SPEED, 32, true, 20, 1.2))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, ENDERMITE_SPEED, 32, 20, 1.2))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 8, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initEnderman() {
        EntityTypes.ENDERMAN = AllayEntityType
                .builder(EntityEndermanImpl.class)
                .vanillaEntity(EntityId.ENDERMAN)
                .addComponent(EntityEndermanBaseComponentImpl::new, EntityEndermanBaseComponentImpl.class)
                .addComponent(EntityEndermanLivingComponentImpl::new, EntityEndermanLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            // Enderman seni diger moblardan cok daha uzaktan fark eder.
                            .sensor(new NearestPlayerSensor(64, 0, 20))
                            // Oncelik 4 (en yuksek): yaralandiktan kisa sure sonra isinlanip kacar.
                            // Burada periyot en az olasilik kadar onemli: isinlanma tek bir tick'te
                            // bitiyor, bu olmadan enderman her tick yeniden degerlendirilir ve
                            // saniyede onlarca kez yok olurdu.
                            .behavior(BehaviorImpl.builder()
                                    .executor(new TeleportAwayExecutor())
                                    .evaluator(all(
                                            new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 40),
                                            new ProbabilityEvaluator(1, 4)
                                    ))
                                    .priority(4)
                                    .period(10)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, ENDERMAN_SPEED, 64, true, 20))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, ENDERMAN_SPEED, 64, 20))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initPiglin() {
        EntityTypes.PIGLIN = AllayEntityType
                .builder(EntityPiglinImpl.class)
                .vanillaEntity(EntityId.PIGLIN)
                .addComponent(EntityPiglinBaseComponentImpl::new, EntityPiglinBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityPiglinLivingComponentImpl::new, EntityPiglinLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(40, 0, 20))
                            // Menzilli ve yakin dovus davranislari ayni oncelikte kayitli; aralarindaki
                            // secimi eldeki silaha bakan evaluator yapiyor. Silah NBT yuklenirken
                            // atiliyor, yani bu bilesen kurulduktan sonra, ustelik bir oyuncu sonradan
                            // degistirebilir; bu yuzden secim burada degil her tick yapilmali.
                            .behavior(BehaviorImpl.builder()
                                    .executor(new RangedAttackExecutor(MemoryTypes.ATTACK_TARGET, PIGLIN_SPEED, 40, 12, 5, true, 40))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            EntityTypeInitializer::holdsCrossbow,
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, PIGLIN_SPEED, 40, true, 30))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> !holdsCrossbow(entity),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new RangedAttackExecutor(MemoryTypes.NEAREST_PLAYER, PIGLIN_SPEED, 40, 12, 5, false, 40))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            EntityTypeInitializer::holdsCrossbow,
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, PIGLIN_SPEED, 40, 30))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> !holdsCrossbow(entity),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initBlaze() {
        EntityTypes.BLAZE = AllayEntityType
                .builder(EntityBlazeImpl.class)
                .vanillaEntity(EntityId.BLAZE)
                .addComponent(EntityBlazeBaseComponentImpl::new, EntityBlazeBaseComponentImpl.class)
                .addComponent(EntityBlazeLivingComponentImpl::new, EntityBlazeLivingComponentImpl.class)
                .addComponent(EntityFlyingPhysicsComponentImpl::new, EntityFlyingPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(48, 0, 20))
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FireballAttackExecutor(
                                            MemoryTypes.ATTACK_TARGET, BLAZE_SPEED, 48,
                                            BLAZE_PREFERRED_RANGE, BLAZE_MIN_RANGE, true,
                                            BLAZE_CHARGE_TIME, BLAZE_COOLDOWN, BLAZE_BURST_SIZE))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FireballAttackExecutor(
                                            MemoryTypes.NEAREST_PLAYER, BLAZE_SPEED, 48,
                                            BLAZE_PREFERRED_RANGE, BLAZE_MIN_RANGE, false,
                                            BLAZE_CHARGE_TIME, BLAZE_COOLDOWN, BLAZE_BURST_SIZE))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 10, 100, false, -1, false, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            // WalkController yok: blaze havada, bu yuzden uc eksendeki hareket
                            // FlyController'dan geliyor. FluctuateController da yok; su blaze'e zarar
                            // veriyor, onu suda sallanmaya itmek olmaz.
                            .controller(new FlyController())
                            .controller(new LookController(true, true))
                            .routeFinder(new SpaceAStarRouteFinder(new FlyingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initCreeper() {
        EntityTypes.CREEPER = AllayEntityType
                .builder(EntityCreeperImpl.class)
                .vanillaEntity(EntityId.CREEPER)
                .addComponent(EntityCreeperBaseComponentImpl::new, EntityCreeperBaseComponentImpl.class)
                .addComponent(EntityCreeperLivingComponentImpl::new, EntityCreeperLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            .behavior(BehaviorImpl.builder()
                                    .executor(new SwellAndExplodeExecutor(
                                            MemoryTypes.ATTACK_TARGET, CREEPER_SPEED, 32,
                                            CREEPER_FUSE_RANGE, true, CREEPER_FUSE_TIME, CREEPER_EXPLOSION_SIZE))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new SwellAndExplodeExecutor(
                                            MemoryTypes.NEAREST_PLAYER, CREEPER_SPEED, 32,
                                            CREEPER_FUSE_RANGE, false, CREEPER_FUSE_TIME, CREEPER_EXPLOSION_SIZE))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initWitch() {
        EntityTypes.WITCH = AllayEntityType
                .builder(EntityWitchImpl.class)
                .vanillaEntity(EntityId.WITCH)
                .addComponent(EntityWitchBaseComponentImpl::new, EntityWitchBaseComponentImpl.class)
                .addComponent(EntityWitchLivingComponentImpl::new, EntityWitchLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            .behavior(BehaviorImpl.builder()
                                    .executor(new PotionAttackExecutor(
                                            MemoryTypes.ATTACK_TARGET, WITCH_SPEED, 32,
                                            WITCH_PREFERRED_RANGE, WITCH_MIN_RANGE, true, WITCH_COOLDOWN))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new PotionAttackExecutor(
                                            MemoryTypes.NEAREST_PLAYER, WITCH_SPEED, 32,
                                            WITCH_PREFERRED_RANGE, WITCH_MIN_RANGE, false, WITCH_COOLDOWN))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initSkeleton() {
        EntityTypes.SKELETON = AllayEntityType
                .builder(EntitySkeletonImpl.class)
                .vanillaEntity(EntityId.SKELETON)
                .addComponent(initInfo -> new EntityArmedBaseComponentImpl(initInfo, () -> ItemTypes.BOW, 0.6, 1.99),
                        EntityArmedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntitySkeletonLivingComponentImpl::new, EntitySkeletonLivingComponentImpl.class)
                .addComponent(EntityUndeadComponentImpl::new, EntityUndeadComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildRangedBehaviorGroup(SKELETON_SPEED, 16, 40,
                        SKELETON_PREFERRED_RANGE, SKELETON_MIN_RANGE, SKELETON_COOLDOWN),
                        EntityAIComponentImpl.class)
                .build();
    }

    public static void initPillager() {
        EntityTypes.PILLAGER = AllayEntityType
                .builder(EntityPillagerImpl.class)
                .vanillaEntity(EntityId.PILLAGER)
                .addComponent(initInfo -> new EntityArmedBaseComponentImpl(initInfo, () -> ItemTypes.CROSSBOW, 0.6, 1.95),
                        EntityArmedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityIllagerLivingComponentImpl::new, EntityIllagerLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildRangedBehaviorGroup(PILLAGER_SPEED, 16, 40,
                        PILLAGER_PREFERRED_RANGE, PILLAGER_MIN_RANGE, PILLAGER_COOLDOWN),
                        EntityAIComponentImpl.class)
                .build();
    }

    public static void initVindicator() {
        EntityTypes.VINDICATOR = AllayEntityType
                .builder(EntityVindicatorImpl.class)
                .vanillaEntity(EntityId.VINDICATOR)
                .addComponent(initInfo -> new EntityArmedBaseComponentImpl(initInfo, () -> ItemTypes.IRON_AXE, 0.6, 1.95),
                        EntityArmedBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityIllagerLivingComponentImpl::new, EntityIllagerLivingComponentImpl.class)
                .addComponent(EntityHumanPhysicsComponentImpl::new, EntityHumanPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestPlayerSensor(16, 0, 20))
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, VINDICATOR_SPEED, 40, true, 20))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                                    ))
                                    .priority(3)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, VINDICATOR_SPEED, 40, 20))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    /**
     * Duz yay/arbalet kullanicilarinin paylastigi davranis grubunu kurar: seni yaralayana ates et,
     * yoksa gorus alanina gireni vur, o da yoksa dolas.
     */
    private static EntityAIComponentImpl buildRangedBehaviorGroup(float speed, double sightRange, double senseRange,
                                                                  double preferredRange, double minRange, int coolDown) {
        var behaviorGroup = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(sightRange, 0, 20))
                .behavior(BehaviorImpl.builder()
                        .executor(new RangedAttackExecutor(MemoryTypes.ATTACK_TARGET, speed, senseRange,
                                preferredRange, minRange, true, coolDown))
                        .evaluator(all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                        ))
                        .priority(3)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(new RangedAttackExecutor(MemoryTypes.NEAREST_PLAYER, speed, senseRange,
                                preferredRange, minRange, false, coolDown))
                        .evaluator(all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                        ))
                        .priority(2)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                .controller(new WalkController())
                .controller(new FluctuateController())
                .controller(new LookController(true, true))
                .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                .build();

        return new EntityAIComponentImpl(behaviorGroup);
    }

    public static void initSulfurCube() {
        EntityTypes.SULFUR_CUBE = AllayEntityType
                .builder(EntitySulfurCubeImpl.class)
                .vanillaEntity(EntityId.SULFUR_CUBE)
                // Kupun icinin nasil gorunecegi bu property'den okunuyor; kaydedilmezse istemci
                // kupu her zaman bos cizer.
                .setProperties(EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE)
                .addComponent(EntitySulfurCubeBaseComponentImpl::new, EntitySulfurCubeBaseComponentImpl.class)
                .addComponent(EntitySulfurCubeLivingComponentImpl::new, EntitySulfurCubeLivingComponentImpl.class)
                .addComponent(EntitySulfurCubePhysicsComponentImpl::new, EntitySulfurCubePhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestAbsorbableSensor(SULFUR_CUBE_SEEK_RANGE, 20))
                            // Oncelik 2: emilebilir bir bloga ya da onu tutan oyuncuya git
                            .behavior(BehaviorImpl.builder()
                                    .executor(new AbsorbBlockExecutor(SULFUR_CUBE_SPEED, SULFUR_CUBE_SEEK_RANGE))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_ABSORBABLE))
                                    .priority(2)
                                    .build())
                            // Oncelik 1 (en dusuk): amacsizca ziplayarak dolas. Blok tasiyan kup
                            // yerinde durur; wiki "emince hareket etmeyi birakir" diyor.
                            .behavior(BehaviorImpl.builder()
                                    // Blok emen kup durur; bunu evaluator degil executor yapmali,
                                    // cunku calisan bir davranisin evaluator'u bir daha bakilmiyor.
                                    .executor(new SulfurCubeRoamExecutor(SULFUR_CUBE_SPEED, 8, 120, false, -1, true, 10))
                                    .evaluator(entity -> !(entity instanceof EntitySulfurCubeBaseComponent cube)
                                                         || cube.getAbsorbedBlock() == null)
                                    .priority(1)
                                    .build())
                            // Yurume degil ziplama: kup yerde bekleyip araliklarla sicriyor ve
                            // havadayken yonunu degistiremiyor.
                            .controller(new CubeJumpController(SULFUR_CUBE_JUMP_SPEED, SULFUR_CUBE_JUMP_HEIGHT,
                                    SULFUR_CUBE_JUMP_INTERVAL, SULFUR_CUBE_JUMP_ANIMATION_TICKS))
                            .controller(new FluctuateController())
                            .controller(new LookController(true, true))
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    /**
     * Bir kup mobunun davranis grubunu kurar: gordugu oyuncuya ziplayarak gider.
     *
     * <p>Balcik ve magma kupu tek farkla ayni sekilde dovusur: ikisi de dokundugu oyuncuya hasar
     * verir, o da yakin dovus executor'unun isi. Ziplayan hareketi ayrica modellemeye gerek yok,
     * yurume kontrolcusu engel gordugunde zaten ziplatiyor.</p>
     *
     * @param speed kovalama hizi
     * @param coolDown vuruslar arasindaki bekleme (tick)
     */
    private static EntityAIComponentImpl buildCubeBehaviorGroup(float speed, int coolDown) {
        var behaviorGroup = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(16, 0, 20))
                .behavior(BehaviorImpl.builder()
                        .executor(new MeleeAttackExecutor(MemoryTypes.ATTACK_TARGET, speed, 32, true, coolDown))
                        .evaluator(all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.ATTACK_TARGET),
                                entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET))
                        ))
                        .priority(3)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_PLAYER, speed, 32, coolDown))
                        .evaluator(all(
                                new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                entity -> isValidHostileTarget(entity, entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER))
                        ))
                        .priority(2)
                        .build())
                .behavior(BehaviorImpl.builder()
                        .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                .controller(new WalkController())
                .controller(new FluctuateController())
                .controller(new LookController(true, true))
                .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                .build();

        return new EntityAIComponentImpl(behaviorGroup);
    }

    public static void initSlime() {
        EntityTypes.SLIME = AllayEntityType
                .builder(EntitySlimeImpl.class)
                .vanillaEntity(EntityId.SLIME)
                .addComponent(EntityCubeBaseComponentImpl::new, EntityCubeBaseComponentImpl.class)
                .addComponent(() -> new EntityCubeLivingComponentImpl(
                        () -> EntityTypes.SLIME, () -> ItemTypes.SLIME_BALL, true),
                        EntityCubeLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildCubeBehaviorGroup(CUBE_SPEED, CUBE_ATTACK_COOLDOWN), EntityAIComponentImpl.class)
                .build();
    }

    public static void initMagmaCube() {
        EntityTypes.MAGMA_CUBE = AllayEntityType
                .builder(EntityMagmaCubeImpl.class)
                .vanillaEntity(EntityId.MAGMA_CUBE)
                .addComponent(EntityCubeBaseComponentImpl::new, EntityCubeBaseComponentImpl.class)
                .addComponent(() -> new EntityCubeLivingComponentImpl(
                        () -> EntityTypes.MAGMA_CUBE, () -> ItemTypes.MAGMA_CREAM, false) {
                    @Override
                    public boolean isFireproof() {
                        // Nether'in kupu kendi elementinden zarar gormez.
                        return true;
                    }
                }, EntityCubeLivingComponentImpl.class)
                .addComponent(EntityMobPhysicsComponentImpl::new, EntityMobPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildCubeBehaviorGroup(CUBE_SPEED, CUBE_ATTACK_COOLDOWN), EntityAIComponentImpl.class)
                .build();
    }

    /**
     * Yuzen bir mobun davranis grubunu kurar: oyuncu yaklasirsa kac, yoksa suda dolas.
     *
     * <p>Balik ve akselotun ortak iskeleti. Kara moblarindan iki noktada ayriliyor: rotayi
     * {@link SpaceAStarRouteFinder} uc boyutlu kuruyor ve hareketi {@link SwimController}
     * uyguluyor, cunku duz yol bulma ile yurume kontrolcusu bir baligi girdigi derinlikte
     * birakirdi. {@code FluctuateController} da yok — o su yuzeyine dogru itiyor ve yuzen bir mobu
     * surekli yuzeye cikarirdi.</p>
     *
     * @param speed yuzme hizi
     * @param fleeRange oyuncuyu bu mesafede fark edip kacar (blok)
     */
    private static EntityAIComponentImpl buildSwimmingBehaviorGroup(float speed, double fleeRange) {
        var posEvaluator = new SwimmingPosEvaluator();
        var behaviorGroup = BehaviorGroupImpl.builder()
                .sensor(new NearestPlayerSensor(fleeRange, 0, 20))
                // Oncelik 2: yaklasan oyuncudan uzaklas
                .behavior(BehaviorImpl.builder()
                        .executor(new SpaceRandomRoamExecutor(speed * 2, 10, 4, 1, 12, posEvaluator))
                        .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER))
                        .priority(2)
                        .build())
                // Oncelik 1 (en dusuk): sakin sakin dolas
                .behavior(BehaviorImpl.builder()
                        .executor(new SpaceRandomRoamExecutor(speed, 8, 3, 40, 12, posEvaluator))
                        .evaluator(entity -> true)
                        .priority(1)
                        .build())
                .controller(new SwimController())
                .controller(new LookController(true, true))
                .routeFinder(new SpaceAStarRouteFinder(posEvaluator))
                .build();

        return new EntityAIComponentImpl(behaviorGroup);
    }

    public static void initCod() {
        EntityTypes.COD = AllayEntityType
                .builder(EntityCodImpl.class)
                .vanillaEntity(EntityId.COD)
                .addComponent(initInfo -> new EntityFishBaseComponentImpl(initInfo, 0.6, 0.3), EntityFishBaseComponentImpl.class)
                .addComponent(() -> new EntityFishLivingComponentImpl(3, () -> ItemTypes.COD), EntityFishLivingComponentImpl.class)
                .addComponent(EntityFishPhysicsComponentImpl::new, EntityFishPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildSwimmingBehaviorGroup(COD_SPEED, FISH_FLEE_RANGE), EntityAIComponentImpl.class)
                .build();
    }

    public static void initSalmon() {
        EntityTypes.SALMON = AllayEntityType
                .builder(EntitySalmonImpl.class)
                .vanillaEntity(EntityId.SALMON)
                .addComponent(initInfo -> new EntityFishBaseComponentImpl(initInfo, 0.5, 0.5), EntityFishBaseComponentImpl.class)
                .addComponent(() -> new EntityFishLivingComponentImpl(3, () -> ItemTypes.SALMON), EntityFishLivingComponentImpl.class)
                .addComponent(EntityFishPhysicsComponentImpl::new, EntityFishPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildSwimmingBehaviorGroup(SALMON_SPEED, SALMON_FLEE_RANGE), EntityAIComponentImpl.class)
                .build();
    }

    public static void initTropicalfish() {
        EntityTypes.TROPICALFISH = AllayEntityType
                .builder(EntityTropicalfishImpl.class)
                .vanillaEntity(EntityId.TROPICALFISH)
                .addComponent(initInfo -> new EntityFishBaseComponentImpl(initInfo, 0.4, 0.4), EntityFishBaseComponentImpl.class)
                .addComponent(() -> new EntityFishLivingComponentImpl(3, () -> ItemTypes.TROPICAL_FISH), EntityFishLivingComponentImpl.class)
                .addComponent(EntityFishPhysicsComponentImpl::new, EntityFishPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildSwimmingBehaviorGroup(TROPICALFISH_SPEED, FISH_FLEE_RANGE), EntityAIComponentImpl.class)
                .build();
    }

    public static void initPufferfish() {
        EntityTypes.PUFFERFISH = AllayEntityType
                .builder(EntityPufferfishImpl.class)
                .vanillaEntity(EntityId.PUFFERFISH)
                .addComponent(initInfo -> new EntityFishBaseComponentImpl(initInfo, 0.8, 0.8), EntityFishBaseComponentImpl.class)
                .addComponent(() -> new EntityFishLivingComponentImpl(3, () -> ItemTypes.PUFFERFISH), EntityFishLivingComponentImpl.class)
                .addComponent(EntityFishPhysicsComponentImpl::new, EntityFishPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> buildSwimmingBehaviorGroup(PUFFERFISH_SPEED, FISH_FLEE_RANGE), EntityAIComponentImpl.class)
                .build();
    }

    public static void initAxolotl() {
        EntityTypes.AXOLOTL = AllayEntityType
                .builder(EntityAxolotlImpl.class)
                .vanillaEntity(EntityId.AXOLOTL)
                .addComponent(EntityAxolotlBaseComponentImpl::new, EntityAxolotlBaseComponentImpl.class)
                .addComponent(EntityAxolotlLivingComponentImpl::new, EntityAxolotlLivingComponentImpl.class)
                .addComponent(EntityAquaticPhysicsComponentImpl::new, EntityAquaticPhysicsComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var posEvaluator = new SwimmingPosEvaluator();
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            // Akselot oyuncuyu umursamaz, balik arar.
                            .sensor(new NearestFishSensor(AXOLOTL_HUNT_RANGE, 20))
                            // Oncelik 2: gordugu baligin pesine dus
                            .behavior(BehaviorImpl.builder()
                                    .executor(new MeleeAttackExecutor(MemoryTypes.NEAREST_FISH, AXOLOTL_SPEED, 16, 20, 1.5))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FISH))
                                    .priority(2)
                                    .build())
                            // Oncelik 1 (en dusuk): suda dolas
                            .behavior(BehaviorImpl.builder()
                                    .executor(new SpaceRandomRoamExecutor(AXOLOTL_SPEED, 8, 3, 40, 12, posEvaluator))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new SwimController())
                            .controller(new LookController(true, true))
                            .routeFinder(new SpaceAStarRouteFinder(posEvaluator))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    /**
     * Varligin su an arbalet tutup tutmadigini soyler; menzilli piglin ile yakin dovus piglinini
     * ayiran sey budur.
     */
    private static boolean holdsCrossbow(EntityIntelligent entity) {
        if (!(entity instanceof EntityContainerHolderComponent containerHolder)) {
            return false;
        }

        var handContainer = containerHolder.getContainer(ContainerTypes.ENTITY_HAND);
        return handContainer != null && handContainer.getItemInHand().getItemType() == ItemTypes.CROSSBOW;
    }

    public static void initXBOrb() {
        EntityTypes.XP_ORB = AllayEntityType
                .builder(EntityXpOrbImpl.class)
                .vanillaEntity(EntityId.XP_ORB)
                .addComponent(EntityXpOrbBaseComponentImpl::new, EntityXpOrbBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean hasFallDamage() {
                            return false;
                        }
                    };
                    component.setMaxHealth(5);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(() -> new EntityPhysicsComponentImpl() {
                    @Override
                    public double getGravity() {
                        return 0.03;
                    }

                    @Override
                    public double getWaterBuoyancy() {
                        return 0.0405;
                    }

                    @Override
                    public double getWaterDragFactor() {
                        return 0.02;
                    }

                    @Override
                    public double getLavaBuoyancy() {
                        return 0.0405;
                    }

                    @Override
                    public double getLavaDragFactor() {
                        return 0.05;
                    }
                }, EntityPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initTnt() {
        EntityTypes.TNT = AllayEntityType
                .builder(EntityTntImpl.class)
                .vanillaEntity(EntityId.TNT)
                .addComponent(EntityTntBaseComponentImpl::new, EntityTntBaseComponentImpl.class)
                .addComponent(EntityTntPhysicsComponentImpl::new, EntityTntPhysicsComponentImpl.class)
                .build();
    }

    public static void initLightningBolt() {
        EntityTypes.LIGHTNING_BOLT = AllayEntityType
                .builder(EntityLightningBoltImpl.class)
                .vanillaEntity(EntityId.LIGHTNING_BOLT)
                .addComponent(EntityLightningBoltBaseComponentImpl::new, EntityLightningBoltBaseComponentImpl.class)
                .build();
    }

    public static void initEnderCrystal() {
        EntityTypes.ENDER_CRYSTAL = AllayEntityType
                .builder(EntityEnderCrystalImpl.class)
                .vanillaEntity(EntityId.ENDER_CRYSTAL)
                .addComponent(EntityEnderCrystalBaseComponentImpl::new, EntityEnderCrystalBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean canBeAttacked(DamageContainer damage) {
                            if (damage.getAttacker() instanceof EntityEnderDragon) {
                                return false;
                            }

                            return super.canBeAttacked(damage);
                        }

                        @Override
                        public boolean hasFireDamage() {
                            return false;
                        }

                        @Override
                        public boolean hasDrowningDamage() {
                            return false;
                        }

                        @Override
                        public boolean isFireproof() {
                            return true;
                        }

                        @Override
                        protected boolean hasDeadTimer() {
                            return false;
                        }
                    };
                    component.setMaxHealth(5);
                    return component;
                }, EntityLivingComponentImpl.class)
                .build();
    }

    public static void initProjectile() {
        EntityTypes.SNOWBALL = AllayEntityType
                .builder(EntitySnowballImpl.class)
                .vanillaEntity(EntityId.SNOWBALL)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntitySnowballPhysicsComponentImpl::new, EntitySnowballPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
        EntityTypes.SPLASH_POTION = AllayEntityType
                .builder(EntitySplashPotionImpl.class)
                .vanillaEntity(EntityId.SPLASH_POTION)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntitySplashPotionPhysicsComponentImpl::new, EntitySplashPotionPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(EntityPotionComponentImpl::new, EntityPotionComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
        EntityTypes.LINGERING_POTION = AllayEntityType
                .builder(EntityLingeringPotionImpl.class)
                .vanillaEntity(EntityId.LINGERING_POTION)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityLingeringPotionPhysicsComponentImpl::new, EntityLingeringPotionPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(EntityPotionComponentImpl::new, EntityPotionComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
        EntityTypes.ENDER_PEARL = AllayEntityType
                .builder(EntityEnderPearlImpl.class)
                .vanillaEntity(EntityId.ENDER_PEARL)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityEnderPearlPhysicsComponentImpl::new, EntityEnderPearlPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
        EntityTypes.XP_BOTTLE = AllayEntityType
                .builder(EntityXpBottleImpl.class)
                .vanillaEntity(EntityId.XP_BOTTLE)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityXpBottlePhysicsComponentImpl::new, EntityXpBottlePhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
        EntityTypes.ARROW = AllayEntityType
                .builder(EntityArrowImpl.class)
                .vanillaEntity(EntityId.ARROW)
                .addComponent(EntityArrowBaseComponentImpl::new, EntityArrowBaseComponentImpl.class)
                .addComponent(EntityArrowPhysicsComponentImpl::new, EntityArrowPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(EntityPotionComponentImpl::new, EntityPotionComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean canBeAttacked(DamageContainer damage) {
                            return damage.getDamageType() == DamageType.API;
                        }

                        @Override
                        public boolean hasFallDamage() {
                            return false;
                        }

                        @Override
                        public boolean hasFireDamage() {
                            return false;
                        }

                        @Override
                        public boolean hasDrowningDamage() {
                            return false;
                        }

                        @Override
                        protected boolean hasDeadTimer() {
                            return false;
                        }
                    };
                    component.setMaxHealth(5);
                    return component;
                }, EntityLivingComponentImpl.class)
                .build();
    }

    public static void initPainting() {
        EntityTypes.PAINTING = AllayEntityType
                .builder(EntityPaintingImpl.class)
                .vanillaEntity(EntityId.PAINTING)
                .addComponent(EntityPaintingBaseComponentImpl::new, EntityPaintingBaseComponentImpl.class)
                .addComponent(EntityLivingComponentImpl::new, EntityLivingComponentImpl.class)
                .build();
    }

    public static void initFireworkRocket() {
        EntityTypes.FIREWORKS_ROCKET = AllayEntityType
                .builder(EntityFireworksRocketImpl.class)
                .vanillaEntity(EntityId.FIREWORKS_ROCKET)
                .addComponent(EntityFireworksRocketPhysicsComponentImpl::new, EntityFireworksRocketPhysicsComponentImpl.class)
                .addComponent(EntityFireworksRocketBaseComponentImpl::new, EntityFireworksRocketBaseComponentImpl.class)
                .build();
    }

    public static void initAreaEffectCloud() {
        EntityTypes.AREA_EFFECT_CLOUD = AllayEntityType
                .builder(EntityAreaEffectCloudImpl.class)
                .vanillaEntity(EntityId.AREA_EFFECT_CLOUD)
                .addComponent(EntityAreaEffectCloudBaseComponentImpl::new, EntityAreaEffectCloudBaseComponentImpl.class)
                .addComponent(EntityPotionComponentImpl::new, EntityPotionComponentImpl.class)
                .build();
    }

    public static void initWindCharge() {
        EntityTypes.WIND_CHARGE_PROJECTILE = AllayEntityType
                .builder(EntityWindChargeProjectileImpl.class)
                .vanillaEntity(EntityId.WIND_CHARGE_PROJECTILE)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityWindChargePhysicsComponentImpl::new, EntityWindChargePhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();

        EntityTypes.BREEZE_WIND_CHARGE_PROJECTILE = AllayEntityType
                .builder(EntityBreezeWindChargeProjectileImpl.class)
                .vanillaEntity(EntityId.BREEZE_WIND_CHARGE_PROJECTILE)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityBreezeWindChargePhysicsComponentImpl::new, EntityBreezeWindChargePhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initFireball() {
        EntityTypes.SMALL_FIREBALL = AllayEntityType
                .builder(EntitySmallFireballImpl.class)
                .vanillaEntity(EntityId.SMALL_FIREBALL)
                .addComponent(EntitySmallFireballBaseComponentImpl::new, EntitySmallFireballBaseComponentImpl.class)
                .addComponent(EntitySmallFireballPhysicsComponentImpl::new, EntitySmallFireballPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
        EntityTypes.FIREBALL = AllayEntityType
                .builder(EntityFireballImpl.class)
                .vanillaEntity(EntityId.FIREBALL)
                .addComponent(EntityFireballBaseComponentImpl::new, EntityFireballBaseComponentImpl.class)
                .addComponent(EntityFireballPhysicsComponentImpl::new, EntityFireballPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initThrownTrident() {
        EntityTypes.THROWN_TRIDENT = AllayEntityType
                .builder(EntityThrownTridentImpl.class)
                .vanillaEntity(EntityId.THROWN_TRIDENT)
                .addComponent(EntityThrownTridentBaseComponentImpl::new, EntityThrownTridentBaseComponentImpl.class)
                .addComponent(EntityThrownTridentPhysicsComponentImpl::new, EntityThrownTridentPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                // Bedrock Edition tridents never despawn - use max integer age
                .addComponent(() -> new EntityAgeComponentImpl(Integer.MAX_VALUE), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initEgg() {
        EntityTypes.EGG = AllayEntityType
                .builder(EntityEggImpl.class)
                .vanillaEntity(EntityId.EGG)
                .setProperties(EntityPropertyTypes.CLIMATE_VARIANT)
                .addComponent(EntityProjectileBaseComponentImpl::new, EntityProjectileBaseComponentImpl.class)
                .addComponent(EntityEggPhysicsComponentImpl::new, EntityEggPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                .addComponent(() -> new EntityAgeComponentImpl(), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initPig() {
        EntityTypes.PIG = AllayEntityType
                .builder(EntityPigImpl.class)
                .vanillaEntity(EntityId.PIG)
                .setProperties(EntityPropertyTypes.CLIMATE_VARIANT)
                .addComponent(EntityPigBaseComponentImpl::new, EntityPigBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl();
                    component.setMaxHealth(10);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAnimalComponentImpl(item ->
                        item.getItemType() == ItemTypes.CARROT
                        || item.getItemType() == ItemTypes.POTATO
                        || item.getItemType() == ItemTypes.BEETROOT
                ), EntityAnimalComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestFeedingPlayerSensor(8))
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            .coreBehavior(BehaviorImpl.builder()
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
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.2875f, 12, 40, true, 100, true, 10))
                                    .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                                    .priority(6)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new EntityBreedingExecutor(100, 0.23f))
                                    .evaluator(entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE))
                                    .priority(5)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FollowEntityExecutor(MemoryTypes.NEAREST_FEEDING_PLAYER, 0.253f, 64, 2.25))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER))
                                    .priority(4)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            new ProbabilityEvaluator(2, 5)
                                    ))
                                    .priority(2)
                                    .period(100)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new LookController(true, true))
                            .controller(new FluctuateController())
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initCow() {
        EntityTypes.COW = AllayEntityType
                .builder(EntityCowImpl.class)
                .vanillaEntity(EntityId.COW)
                .setProperties(EntityPropertyTypes.CLIMATE_VARIANT)
                .addComponent(EntityCowBaseComponentImpl::new, EntityCowBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl();
                    component.setMaxHealth(10);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAnimalComponentImpl(item -> item.getItemType() == ItemTypes.WHEAT), EntityAnimalComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestFeedingPlayerSensor(8))
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            .coreBehavior(BehaviorImpl.builder()
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
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.2875f, 12, 40, true, 100, true, 10))
                                    .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                                    .priority(6)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new EntityBreedingExecutor(100, 0.23f))
                                    .evaluator(entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE))
                                    .priority(5)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FollowEntityExecutor(MemoryTypes.NEAREST_FEEDING_PLAYER, 0.253f, 64, 2.25))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER))
                                    .priority(4)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            new ProbabilityEvaluator(2, 5)
                                    ))
                                    .priority(2)
                                    .period(100)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new LookController(true, true))
                            .controller(new FluctuateController())
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initChicken() {
        EntityTypes.CHICKEN = AllayEntityType
                .builder(EntityChickenImpl.class)
                .vanillaEntity(EntityId.CHICKEN)
                .setProperties(EntityPropertyTypes.CLIMATE_VARIANT)
                .addComponent(EntityChickenBaseComponentImpl::new, EntityChickenBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl() {
                        @Override
                        public boolean hasFallDamage() {
                            // Chicken do not have fall damage
                            return false;
                        }
                    };
                    component.setMaxHealth(4);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityChickenPhysicsComponentImpl::new, EntityChickenPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAnimalComponentImpl(item ->
                        item.getItemType() == ItemTypes.WHEAT_SEEDS ||
                        item.getItemType() == ItemTypes.MELON_SEEDS ||
                        item.getItemType() == ItemTypes.PUMPKIN_SEEDS ||
                        item.getItemType() == ItemTypes.BEETROOT_SEEDS ||
                        item.getItemType() == ItemTypes.TORCHFLOWER_SEEDS ||
                        item.getItemType() == ItemTypes.PITCHER_POD
                ), EntityAnimalComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestFeedingPlayerSensor(8))
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            .coreBehavior(BehaviorImpl.builder()
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
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(entity -> {
                                        entity.getMemoryStorage().put(MemoryTypes.LAST_EGG_SPAWN_TIME, entity.getTick());
                                        entity.getDimension().dropItem(ItemTypes.EGG.createItemStack(1), entity.getLocation());
                                        entity.getDimension().addSound(entity.getLocation(), SimpleSound.EGG_LAY);
                                        return false;
                                    })
                                    .evaluator(all(
                                            entity -> !(entity instanceof EntityBabyComponent babyComponent && babyComponent.isBaby()),
                                            any(
                                                    all(
                                                            new PassByTimeEvaluator(MemoryTypes.LAST_EGG_SPAWN_TIME, 6000, 12000),
                                                            new ProbabilityEvaluator(20, 100)
                                                    ),
                                                    new PassByTimeEvaluator(MemoryTypes.LAST_EGG_SPAWN_TIME, 12000, Integer.MAX_VALUE)
                                            )
                                    ))
                                    .priority(1)
                                    .period(20)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.2875f, 12, 40, true, 100, true, 10))
                                    .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                                    .priority(6)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new EntityBreedingExecutor(100, 0.15f))
                                    .evaluator(entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE))
                                    .priority(5)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FollowEntityExecutor(MemoryTypes.NEAREST_FEEDING_PLAYER, 0.253f, 64, 2.25))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER))
                                    .priority(4)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            new ProbabilityEvaluator(2, 5)
                                    ))
                                    .priority(2)
                                    .period(100)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.11f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new LookController(true, true))
                            .controller(new FluctuateController())
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initFox() {
        EntityTypes.FOX = AllayEntityType
                .builder(EntityFoxImpl.class)
                .vanillaEntity(EntityId.FOX)
                .addComponent(EntityFoxBaseComponentImpl::new, EntityFoxBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl();
                    component.setMaxHealth(10);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAnimalComponentImpl(item ->
                        item.getItemType() == ItemTypes.SWEET_BERRIES
                        || item.getItemType() == ItemTypes.GLOW_BERRIES
                ), EntityAnimalComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            .sensor(new NearestFeedingPlayerSensor(16))
                            .sensor(new NearestPlayerSensor(6, 0, 20))
                            .coreBehavior(BehaviorImpl.builder()
                                    .executor(new InLoveExecutor(400))
                                    .evaluator(all(
                                            entity -> !entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE),
                                            entity -> {
                                                var lastLoveTime = entity.getMemoryStorage().get(MemoryTypes.LAST_IN_LOVE_TIME);
                                                return lastLoveTime == null || lastLoveTime <= 0
                                                        || entity.getTick() - lastLoveTime >= 6000;
                                            },
                                            new PassByTimeEvaluator(MemoryTypes.LAST_BE_FEED_TIME, 0, 400)
                                    ))
                                    .priority(1)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(
                                            FOX_PANIC_SPEED, 12, 40, true, 100, true, 10))
                                    .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                                    .priority(6)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new EntityBreedingExecutor(100, FOX_BASE_SPEED))
                                    .evaluator(entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE))
                                    .priority(5)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FollowEntityExecutor(
                                            MemoryTypes.NEAREST_FEEDING_PLAYER, FOX_TEMPT_SPEED, 256, 2.25))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER))
                                    .priority(4)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            new ProbabilityEvaluator(2, 100)
                                    ))
                                    .priority(2)
                                    .build())
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(
                                            FOX_STROLL_SPEED, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            .controller(new WalkController())
                            .controller(new LookController(true, true))
                            .controller(new FluctuateController())
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    public static void initFishingHook() {
        EntityTypes.FISHING_HOOK = AllayEntityType
                .builder(EntityFishingHookImpl.class)
                .vanillaEntity(EntityId.FISHING_HOOK)
                .addComponent(EntityFishingHookBaseComponentImpl::new, EntityFishingHookBaseComponentImpl.class)
                .addComponent(EntityFishingHookPhysicsComponentImpl::new, EntityFishingHookPhysicsComponentImpl.class)
                .addComponent(EntityProjectileComponentImpl::new, EntityProjectileComponentImpl.class)
                // Fishing hook despawns after 60 seconds if not used
                .addComponent(() -> new EntityAgeComponentImpl(1200), EntityAgeComponentImpl.class)
                .build();
    }

    public static void initArmorStand() {
        EntityTypes.ARMOR_STAND = AllayEntityType
                .builder(EntityArmorStandImpl.class)
                .vanillaEntity(EntityId.ARMOR_STAND)
                .addComponent(EntityArmorStandBaseComponentImpl::new, EntityArmorStandBaseComponentImpl.class)
                .addComponent(EntityHumanLikeContainerHolderComponentImpl::new, EntityHumanLikeContainerHolderComponentImpl.class)
                .addComponent(EntityArmorStandLivingComponentImpl::new, EntityArmorStandLivingComponentImpl.class)
                .addComponent(() -> new EntityHumanPhysicsComponentImpl() {
                    @Override
                    public void onFall(double fallDistance) {
                        super.onFall(fallDistance);
                        if (fallDistance >= 3) {
                            thisEntity.getDimension().addSound(thisEntity.getLocation(), SimpleSound.ARMOR_STAND_LAND);
                        }
                    }
                }, EntityHumanPhysicsComponentImpl.class)
                .build();
    }

    public static void initSheep() {
        EntityTypes.SHEEP = AllayEntityType
                .builder(EntitySheepImpl.class)
                .vanillaEntity(EntityId.SHEEP)
                .addComponent(EntitySheepBaseComponentImpl::new, EntitySheepBaseComponentImpl.class)
                .addComponent(() -> {
                    var component = new EntityLivingComponentImpl();
                    component.setMaxHealth(8);
                    return component;
                }, EntityLivingComponentImpl.class)
                .addComponent(EntityAnimalPhysicsComponentImpl::new, EntityAnimalPhysicsComponentImpl.class)
                .addComponent(() -> new EntityAnimalComponentImpl(item -> item.getItemType() == ItemTypes.WHEAT), EntityAnimalComponentImpl.class)
                .addComponent(EntityBabyComponentImpl::new, EntityBabyComponentImpl.class)
                .addComponent(EntityHeadYawComponentImpl::new, EntityHeadYawComponentImpl.class)
                .addComponent(EntityParallelTickComponentImpl::new, EntityParallelTickComponentImpl.class)
                .addComponent(() -> new EntityDyeableComponentImpl(() -> {
                    var rand = ThreadLocalRandom.current();
                    int roll = rand.nextInt(100);
                    if (roll < 82) return DyeColor.WHITE;
                    if (roll < 87) return DyeColor.BLACK;
                    if (roll < 92) return DyeColor.GRAY;
                    if (roll < 97) return DyeColor.LIGHT_GRAY;
                    if (roll < 99) return DyeColor.BROWN;
                    return DyeColor.PINK;
                }), EntityDyeableComponentImpl.class)
                .addComponent(() -> {
                    var behaviorGroup = BehaviorGroupImpl.builder()
                            // Sensors
                            .sensor(new NearestFeedingPlayerSensor(8))
                            .sensor(new NearestPlayerSensor(8, 0, 20))
                            // Core behavior: enter love mode when fed
                            .coreBehavior(BehaviorImpl.builder()
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
                                    .build())
                            // Priority 6 (highest): flee when attacked
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.2875f, 12, 40, true, 100, true, 10))
                                    .evaluator(new PassByTimeEvaluator(EntityIntelligent::getLastDamageTime, 0, 100))
                                    .priority(6)
                                    .build())
                            // Priority 5: breed when in love
                            .behavior(BehaviorImpl.builder()
                                    .executor(new EntityBreedingExecutor(100, 0.23f))
                                    .evaluator(entity -> entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE))
                                    .priority(5)
                                    .build())
                            // Priority 4: follow player holding wheat
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FollowEntityExecutor(MemoryTypes.NEAREST_FEEDING_PLAYER, 0.253f, 64, 2.25))
                                    .evaluator(new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_FEEDING_PLAYER))
                                    .priority(4)
                                    .build())
                            // Priority 3: eat grass
                            .behavior(BehaviorImpl.builder()
                                    .executor(new SheepEatGrassExecutor())
                                    .evaluator(all(
                                            any(
                                                    all(
                                                            entity -> entity instanceof EntityAnimal animal && animal.isBaby(),
                                                            new ProbabilityEvaluator(86, 100)
                                                    ),
                                                    all(
                                                            entity -> !(entity instanceof EntityAnimal animal && animal.isBaby()),
                                                            new ProbabilityEvaluator(1, 100)
                                                    )
                                            ),
                                            any(
                                                    new BlockCheckEvaluator(BlockTypes.SHORT_GRASS, new Vector3i(0, 0, 0)),
                                                    new BlockCheckEvaluator(BlockTypes.GRASS_BLOCK, new Vector3i(0, -1, 0))
                                            )
                                    ))
                                    .priority(3)
                                    .period(100)
                                    .build())
                            // Priority 2: look at nearest player
                            .behavior(BehaviorImpl.builder()
                                    .executor(new LookAtEntityExecutor(MemoryTypes.NEAREST_PLAYER, 100))
                                    .evaluator(all(
                                            new MemoryCheckNotEmptyEvaluator(MemoryTypes.NEAREST_PLAYER),
                                            new ProbabilityEvaluator(2, 5)
                                    ))
                                    .priority(2)
                                    .period(100)
                                    .build())
                            // Oncelik 1 (en dusuk): rastgele dolasma
                            .behavior(BehaviorImpl.builder()
                                    .executor(new FlatRandomRoamExecutor(0.1f, 12, 100, false, -1, true, 10))
                                    .evaluator(entity -> true)
                                    .priority(1)
                                    .build())
                            // Controllers
                            .controller(new WalkController())
                            .controller(new LookController(true, true))
                            .controller(new FluctuateController())
                            // Route finder
                            .routeFinder(new FlatAStarRouteFinder(new WalkingPosEvaluator()))
                            .build();

                    return new EntityAIComponentImpl(behaviorGroup);
                }, EntityAIComponentImpl.class)
                .build();
    }

    /**
     * Hatirlanan bir hedefin hala saldirmaya deger olup olmadigini soyler: yasayan bir oyuncu
     * olmali, mobun kendisi olmamali ve yaratici ya da izleyici modda bulunmamali.
     *
     * <p>Oyuncu olmayanlar dogrudan reddedilir. Buradaki moblarin birbiriyle dovusmemesi gerekiyor
     * (nedeni icin {@code EntityHostileLivingComponentImpl}) ve onlari hedef dogrulamasi asamasinda
     * da reddetmek, basibos bir hedef kimliginin bir mob dalgasini kendi icine dondurmesini
     * tumden imkansiz kiliyor.</p>
     *
     * <h2>GearsMC sapmasi: bulanti saldiriyi keser</h2>
     * <p>Bulanti etkisi altindaki mob hicbir hedefi gecerli saymaz. Vanilla'da boyle bir kural
     * yok; bu HeartCore'un Axii Tasi davranisidir ({@code MobEntity::attemptAttack}, bulanti
     * varsa erken donuyordu). Kural motora tasindi cunku Allay'de mob yapay zekasi motorun
     * kendisinde: hedef dogrulamasi hem hatirlanan hedefi (oncelik 3) hem de sensorun buldugu
     * en yakin oyuncuyu (oncelik 2) geciyor, ikisi de elenince mob {@code FlatRandomRoamExecutor}
     * dalina dusuyor — PHP'de de bulantili mob amacsizca dolasiyordu.</p>
     *
     * <p>Vanilla oyunda mobun bulanti kapmasinin bir yolu olmadigi icin bu kural yalnizca
     * eklentinin bilerek verdigi bulantida devreye girer.</p>
     */
    private static boolean isValidHostileTarget(EntityIntelligent entity, long targetId) {
        if (entity.hasEffect(EffectTypes.NAUSEA)) {
            return false;
        }

        var target = entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityPlayer player) || target == entity || !target.isAlive()) {
            return false;
        }

        return switch (player.getGameMode()) {
            case SURVIVAL, ADVENTURE -> true;
            case CREATIVE, SPECTATOR -> false;
        };
    }
}
