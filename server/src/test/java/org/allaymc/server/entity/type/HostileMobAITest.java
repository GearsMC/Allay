package org.allaymc.server.entity.type;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityArrow;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.server.entity.ai.executor.RangedAttackExecutor;
import org.allaymc.server.entity.component.EntityHostileLivingComponentImpl;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityCreeperBaseComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityWeaponStanceComponent;
import org.allaymc.api.entity.data.WeaponStance;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.component.ItemCrossbowBaseComponent;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Kurt, enderman, endermite, piglin ve blaze'in sessizce {@code EntityTypeDefaultInitializer}
 * icindeki bilesensiz taslaklara geri dusmesini engeller. Oradaki kayit bir {@code == null}
 * kontroluyle korundugu icin bir yeniden adlandirma ya da silinen bir {@code initXxx()} derlemeyi
 * bozmaz; moblar sessizce yeniden kipirdamaz hale gelirdi.
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
     * Istemci yaylari, arbaletleri ve kalkik illager silahlarini varlik bayraklarindan canlandirir;
     * bu bayraklar da yalnizca bu bileseni tasiyan moblar icin yaziliyor. Bileseni kaybetmek
     * sorunsuz derlenir ve moblari silahlari indirik dovusur halde birakir, bu yuzden burada
     * sabitlendi.
     */
    @Test
    void armedMobsCarryWeaponStance() {
        assertStartsIdle(EntityTypes.SKELETON);
        assertStartsIdle(EntityTypes.PILLAGER);
        assertStartsIdle(EntityTypes.VINDICATOR);
        assertStartsIdle(EntityTypes.PIGLIN);
    }

    /**
     * Bir arbalet istemcide ancak esyanin kendisi {@code chargedItem} etiketi tasidigi surece
     * gerilmis gorunur; varlik bayraklari bunu tek basina yapamaz. Bu test menzilli executor'un
     * dayandigi mekanizmayi adim adim yuruyor: mobun arbaletini doldur, istemciye serilestirilecek
     * etiket ortaya cikmali.
     */
    @Test
    void loadingAMobsCrossbowMarksTheItemAsCharged() {
        var pillager = EntityTypes.PILLAGER.createEntity(EntityInitInfo.builder().build());
        var handContainer = assertInstanceOf(EntityContainerHolderComponent.class, pillager)
                .getContainer(ContainerTypes.ENTITY_HAND);
        var crossbow = assertInstanceOf(ItemCrossbowBaseComponent.class, handContainer.getItemInHand());

        assertFalse(crossbow.isLoaded(), "pillager bos bir arbaletle dogmali");
        assertFalse(handContainer.getItemInHand().saveExtraTag().containsKey("chargedItem"));

        crossbow.setLoadedProjectile(ItemTypes.ARROW.createItemStack());

        assertTrue(crossbow.isLoaded());
        assertTrue(handContainer.getItemInHand().saveExtraTag().containsKey("chargedItem"),
                "bu etiket olmadan dolu durumu istemciye hic ulasmiyor");
    }

    /**
     * Moblar istemeden surekli birbirini yaraliyor: kalabaligin ortasinda patlayan bir creeper,
     * sapan bir ok, yanindakine degen bir ates topu. Bunlarin hicbiri onlari birbirine
     * dusurmemeli; yoksa bir mob blok dalgasi oyuncu daha gelmeden kendini yok eder. Moblar
     * arasindaki hasar hala isliyor; yalnizca kin tutulmuyor.
     */
    @Test
    void onlyPlayersAreWorthRetaliatingAgainst() {
        var mob = EntityTypes.SKELETON.createEntity(EntityInitInfo.builder().build());
        assertFalse(EntityHostileLivingComponentImpl.isRetaliationTarget(mob),
                "baska bir mob tarafindan vurulan bir mob onu avlamaya baslamamali");

        var player = mock(EntityPlayer.class);
        when(player.isAlive()).thenReturn(true);
        assertTrue(EntityHostileLivingComponentImpl.isRetaliationTarget(player),
                "mob kendisine vuran oyuncuya hala karsilik vermeli");

        assertFalse(EntityHostileLivingComponentImpl.isRetaliationTarget(null),
                "hicbir varliktan gelmeyen hasarda karsilik verilecek kimse yoktur");
    }

    /**
     * Dusman ok degil, onu atandir. Bu olmadan vurulan bir mob okcunun degil okun son konumunun
     * ustune kosar.
     */
    @Test
    void projectilesResolveBackToTheirShooter() {
        var shooter = mock(EntityPlayer.class);
        var arrow = mock(EntityArrow.class);
        when(arrow.getShooter()).thenReturn(shooter);

        assertEquals(shooter, EntityHostileLivingComponentImpl.resolveAttacker(arrow));
    }

    /**
     * Atis sesi de arbalet doldurma adimi da bu kontrole bagli, yani iskeletin arbalet kullanicisi
     * sanilmasi hem goze hem kulaga carpiyor.
     */
    @Test
    void bowAndCrossbowUsersAreToldApart() {
        var weaponReader = new WeaponReadingExecutor();

        var skeleton = assertInstanceOf(EntityIntelligent.class,
                EntityTypes.SKELETON.createEntity(EntityInitInfo.builder().build()));
        assertFalse(weaponReader.holdsCrossbow(skeleton), "iskelet arbalet degil yay tasir");

        var pillager = assertInstanceOf(EntityIntelligent.class,
                EntityTypes.PILLAGER.createEntity(EntityInitInfo.builder().build()));
        assertTrue(weaponReader.holdsCrossbow(pillager), "pillager arbalet tasir");
    }

    /**
     * Executor'un silah kontrolunu disariya acar; aksi halde yalnizca dovus sirasinda erisilebilir.
     */
    private static final class WeaponReadingExecutor extends RangedAttackExecutor {
        private WeaponReadingExecutor() {
            super(MemoryTypes.ATTACK_TARGET, 0.1f, 16, 10, 4, false, 20);
        }

        @Override
        public boolean holdsCrossbow(EntityIntelligent entity) {
            return super.holdsCrossbow(entity);
        }
    }

    @Test
    void creeperStartsWithAnUnlitFuse() {
        var creeper = assertInstanceOf(EntityCreeperBaseComponent.class,
                EntityTypes.CREEPER.createEntity(EntityInitInfo.builder().build()));
        assertFalse(creeper.isSwelling(), "yeni dogmus bir creeper zaten sismis olmamali");
    }

    @Test
    void blazeFliesAndIgnoresFire() {
        var blaze = EntityTypes.BLAZE.createEntity(EntityInitInfo.builder().build());

        var living = assertInstanceOf(EntityLivingComponent.class, blaze);
        assertTrue(living.isFireproof(), "blaze kendi elementinden zarar gormemeli");

        var physics = assertInstanceOf(EntityPhysicsComponent.class, blaze);
        assertEquals(0, physics.getGravity(), "blaze havada durur, yercekimi onu asagi cekmemeli");
    }

    @Test
    void piglinSpawnsWithACrossbowOrAGoldenSword() {
        var seen = new HashSet<Object>();
        // Silah her dogusta yazi tura; bu kadar dogus icinde ikisinden yalnizca birini gormek
        // sansizlik degil, atisin bozuk oldugu anlamina gelir.
        for (int i = 0; i < 100; i++) {
            var piglin = EntityTypes.PIGLIN.createEntity(EntityInitInfo.builder().build());
            var containerHolder = assertInstanceOf(EntityContainerHolderComponent.class, piglin);
            var weapon = containerHolder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType();
            assertTrue(weapon == ItemTypes.CROSSBOW || weapon == ItemTypes.GOLDEN_SWORD,
                    "piglin su silahi tutarak dogdu: " + weapon.getIdentifier());
            seen.add(weapon);
        }

        assertEquals(2, seen.size(), "piglinler hep tek bir silah varyanti atti");
    }

    private static void assertHasBehaviors(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var aiComponent = assertInstanceOf(EntityAIComponent.class, entity,
                entityType.getIdentifier() + " AI bileseni tasimiyor");
        assertFalse(aiComponent.getBehaviorGroup().getBehaviors().isEmpty(),
                entityType.getIdentifier() + " bos bir davranis grubuna sahip");
    }

    private static void assertStartsIdle(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var armed = assertInstanceOf(EntityWeaponStanceComponent.class, entity,
                entityType.getIdentifier() + " silah durusu bildiremiyor, yani silahi hic canlanmayacak");
        assertEquals(WeaponStance.IDLE, armed.getWeaponStance(),
                entityType.getIdentifier() + " daha basta silahini cekmis durumda");
        assertFalse(armed.isAggressive(),
                entityType.getIdentifier() + " daha basta saldirgan durumda");
    }

    private static void assertHoldsWeapon(EntityType<?> entityType, ItemType<?> expected) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var containerHolder = assertInstanceOf(EntityContainerHolderComponent.class, entity,
                entityType.getIdentifier() + " hicbir sey tutamiyor");
        assertEquals(expected, containerHolder.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand().getItemType(),
                entityType.getIdentifier() + " yanlis silahla dogdu");
    }

    private static float maxHealthOf(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        return assertInstanceOf(EntityLivingComponent.class, entity).getMaxHealth();
    }
}
