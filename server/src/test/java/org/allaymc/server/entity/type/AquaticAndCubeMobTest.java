package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.behavior.Behavior;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityCubeBaseComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Yuzen moblarin ve kup moblarinin {@code EntityTypeDefaultInitializer} icindeki bilesensiz
 * taslaklara geri dusmesini engeller; oradaki kayit {@code == null} kontroluyle korundugu icin
 * silinen bir {@code initXxx()} derlemeyi bozmaz, moblar sessizce kipirdamaz hale gelir.
 */
@ExtendWith(AllayTestExtension.class)
public class AquaticAndCubeMobTest {

    @Test
    void aquaticAndCubeMobsHaveBehaviors() {
        assertHasBehaviors(EntityTypes.COD);
        assertHasBehaviors(EntityTypes.SALMON);
        assertHasBehaviors(EntityTypes.TROPICALFISH);
        assertHasBehaviors(EntityTypes.PUFFERFISH);
        assertHasBehaviors(EntityTypes.AXOLOTL);
        assertHasBehaviors(EntityTypes.SLIME);
        assertHasBehaviors(EntityTypes.MAGMA_CUBE);
    }

    /**
     * Balik icin bogulma tersine isler: motorun kurali "gozler suyun altindaysa nefes alamaz"
     * diyor, ki bu bir baligi kendi evinde bogardi.
     */
    @Test
    void fishBreatheUnderwaterAndDoNotDrown() {
        for (var type : new EntityType<?>[]{EntityTypes.COD, EntityTypes.SALMON,
                EntityTypes.TROPICALFISH, EntityTypes.PUFFERFISH, EntityTypes.AXOLOTL}) {
            var living = assertInstanceOf(EntityLivingComponent.class,
                    type.createEntity(EntityInitInfo.builder().build()));
            assertTrue(living.canBreathe(), type.getIdentifier() + " suda nefes alamiyor");
            assertFalse(living.hasDrowningDamage(), type.getIdentifier() + " suda boguluyor");
        }
    }

    /**
     * Suyun icinde varlik notr yuzerlikte olmali: kaldirma kuvveti onu surekli yuzeye
     * tirmandirsaydi hicbir balik dipte kalamazdi.
     */
    @Test
    void fishAreNeutrallyBuoyant() {
        var physics = assertInstanceOf(EntityPhysicsComponent.class,
                EntityTypes.COD.createEntity(EntityInitInfo.builder().build()));
        assertEquals(0, physics.getWaterBuoyancy(), "balik yuzeye itiliyor");
    }

    /**
     * Kupun butun olculeri boyutundan turer ve boyut uc ayri anda belirlenebilir: dogal dogum,
     * diskten yukleme, bolunme. Canin bunlarin hepsine yetismesi, boyut degisimini duyuran tek bir
     * olaya bagli.
     */
    @Test
    void cubeHealthFollowsItsSize() {
        var slime = EntityTypes.SLIME.createEntity(EntityInitInfo.builder().build());
        var cube = assertInstanceOf(EntityCubeBaseComponent.class, slime);
        var living = assertInstanceOf(EntityLivingComponent.class, slime);

        cube.setCubeSize(4);
        assertEquals(16, living.getMaxHealth(), "buyuk kupun cani boyutunun karesi olmali");
        assertEquals(16, living.getHealth(), "boyut degisince can dolu olmali");

        cube.setCubeSize(1);
        assertEquals(1, living.getMaxHealth(), "kucuk kupun cani boyutunun karesi olmali");
    }

    /**
     * Carpisma kutusu da boyutla buyumeli; yoksa buyuk balcik kucucuk bir kutuyla dolasir ve
     * vurulmasi imkansizlasir.
     */
    @Test
    void cubeHitboxFollowsItsSize() {
        var slime = EntityTypes.SLIME.createEntity(EntityInitInfo.builder().build());
        var cube = assertInstanceOf(EntityCubeBaseComponent.class, slime);

        cube.setCubeSize(1);
        var small = slime.getAABB().maxY() - slime.getAABB().minY();

        cube.setCubeSize(4);
        var large = slime.getAABB().maxY() - slime.getAABB().minY();

        assertTrue(large > small, "buyuk kupun carpisma kutusu kucugununkinden buyuk olmali");
    }

    /**
     * Dogal dogumda uc boyuttan biri cikmali; hep ayni boyut cikiyorsa atis bozuktur.
     */
    @Test
    void cubesSpawnInEveryVanillaSize() {
        var seen = new HashSet<Integer>();
        for (int i = 0; i < 200; i++) {
            var slime = EntityTypes.SLIME.createEntity(EntityInitInfo.builder().build());
            seen.add(assertInstanceOf(EntityCubeBaseComponent.class, slime).getCubeSize());
        }

        assertEquals(3, seen.size(), "kupler her vanilla boyutunda dogmuyor: " + seen);
    }

    /**
     * Resmi tanimda dort balik turunun de fizigi {@code "minecraft:physics": {"has_gravity":
     * false}}. Bu kosulsuz bir kural: balik sudan cikarilinca da dusmez, havada asili kalip
     * cirpinir. Testin dunyaya hic dokunmadan calisabilmesi zaten bunun kaniti — "suyun icinde mi"
     * diye bakan bir uygulama burada bir boyut arar ve patlardi.
     */
    @Test
    void fishAreNeverAffectedByGravity() {
        for (var type : new EntityType<?>[]{EntityTypes.COD, EntityTypes.SALMON,
                EntityTypes.TROPICALFISH, EntityTypes.PUFFERFISH}) {
            var physics = assertInstanceOf(EntityPhysicsComponent.class,
                    type.createEntity(EntityInitInfo.builder().build()));
            assertEquals(0, physics.getGravity(),
                    type.getIdentifier() + " yercekiminden etkileniyor");
        }
    }

    /**
     * Carpisma kutulari resmi {@code minecraft:collision_box} degerleri. Gorunmez ama her seyi
     * etkiliyorlar: mobun vurulabilirligi, dar bir gecitten gecip gecemedigi, yol bulmasi.
     */
    @Test
    void aquaticCollisionBoxesMatchVanilla() {
        assertCollisionBox(EntityTypes.COD, 0.6, 0.3);
        assertCollisionBox(EntityTypes.SALMON, 0.5, 0.5);
        assertCollisionBox(EntityTypes.TROPICALFISH, 0.4, 0.4);
        assertCollisionBox(EntityTypes.PUFFERFISH, 0.8, 0.8);
        assertCollisionBox(EntityTypes.AXOLOTL, 0.75, 0.42);
    }

    /**
     * Akselot vurulunca oyuncuyu kovalamamali.
     *
     * <p>Resmi davranis paketinde akselotun {@code minecraft:behavior.hurt_by_target} bileseni hic
     * yok; hedef listesi yalnizca suda bulunan murekkep baligi, balik, iribas ile bogulmus ve
     * muhafiz ailelerinden olusuyor. Yani oyuncu hicbir kosulda hedef degil.</p>
     *
     * <p>Testin dogrudan {@code attack()} cagirmamasinin sebebi: dogurulmamis bir varlik canli
     * sayilmadigi icin saldiri en basta reddediliyor ve boyle bir test yanlis sebeple gecerdi.
     * Onun yerine hafizaya saldirgan yazildiginda yeni bir davranisin acilip acilmadigina
     * bakiliyor. Son adim da testin kendisini dogruluyor: balik hafizasi <em>gercekten</em> bir
     * davranis aciyor, yani olcum calisiyor.</p>
     */
    @Test
    void axolotlNeverChasesThePlayerAfterBeingHit() {
        var axolotl = EntityTypes.AXOLOTL.createEntity(EntityInitInfo.builder().build());
        var ai = assertInstanceOf(EntityAIComponent.class, axolotl);
        var intelligent = assertInstanceOf(EntityIntelligent.class, axolotl);
        var behaviors = ai.getBehaviorGroup().getBehaviors();

        var idle = countRunnable(behaviors, intelligent);

        ai.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, 1L);
        assertEquals(idle, countRunnable(behaviors, intelligent),
                "akselot kendisine vurani kovalamaya basliyor");

        ai.getMemoryStorage().put(MemoryTypes.NEAREST_FISH, 1L);
        assertEquals(idle + 1, countRunnable(behaviors, intelligent),
                "kontrol basarisiz: hafiza degisimi hicbir davranisi acmiyor, yani test bir sey olcmuyor");
    }

    private static long countRunnable(Collection<? extends Behavior> behaviors, EntityIntelligent entity) {
        return behaviors.stream().filter(behavior -> behavior.evaluate(entity)).count();
    }

    private static void assertCollisionBox(EntityType<?> entityType, double width, double height) {
        var aabb = entityType.createEntity(EntityInitInfo.builder().build()).getBaseAABB();
        assertEquals(width, aabb.maxX() - aabb.minX(), 1e-9,
                entityType.getIdentifier() + " genisligi vanilla ile uyusmuyor");
        assertEquals(height, aabb.maxY() - aabb.minY(), 1e-9,
                entityType.getIdentifier() + " yuksekligi vanilla ile uyusmuyor");
    }

    private static void assertHasBehaviors(EntityType<?> entityType) {
        var entity = entityType.createEntity(EntityInitInfo.builder().build());
        var aiComponent = assertInstanceOf(EntityAIComponent.class, entity,
                entityType.getIdentifier() + " AI bileseni tasimiyor");
        assertFalse(aiComponent.getBehaviorGroup().getBehaviors().isEmpty(),
                entityType.getIdentifier() + " bos bir davranis grubuna sahip");
    }
}
