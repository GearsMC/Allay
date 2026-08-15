package org.allaymc.server.entity.type;

import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.world.WorldViewer;
import org.allaymc.server.entity.ai.executor.SulfurCubeRoamExecutor;
import org.allaymc.server.entity.component.sulfurcube.EntitySulfurCubeBaseComponentImpl;
import org.allaymc.server.entity.component.sulfurcube.SulfurCubeArchetypes;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Sulfur kupunun kendine ozgu mekanigini sabitler: emdigi blok yalnizca gorunumunu degil cani,
 * fizigi ve hasara bagisikligini da degistiriyor.
 */
@ExtendWith(AllayTestExtension.class)
public class SulfurCubeTest {

    @Test
    void sulfurCubeHasBehaviors() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var ai = assertInstanceOf(EntityAIComponent.class, cube, "sulfur kupu AI bileseni tasimiyor");
        assertFalse(ai.getBehaviorGroup().getBehaviors().isEmpty(), "davranis grubu bos");
    }

    /**
     * Buyuk kup 8, kucuk kup 4 canli. Boyut uc ayri anda belirlenebildigi icin canin ona yetismesi
     * boyut degisimini duyuran tek bir olaya bagli.
     */
    @Test
    void healthFollowsSize() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        var living = assertInstanceOf(EntityLivingComponent.class, cube);

        assertTrue(base.isLarge(), "sulfur kupu buyuk dogmali");
        assertEquals(8, living.getMaxHealth(), "buyuk kupun cani 8 olmali");

        base.setLarge(false);
        assertEquals(4, living.getMaxHealth(), "kucuk kupun cani 4 olmali");
    }

    /**
     * Kucuk kupler blok ememez; wiki bunu acikca soyluyor. Kuculen bir kup tasidigini birakmali.
     */
    @Test
    void smallCubesCannotHoldBlocks() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);

        base.setAbsorbedBlock(BlockTypes.TNT.getDefaultState());
        assertNotNull(base.getAbsorbedBlock(), "buyuk kup blok emebilmeli");

        base.setLarge(false);
        assertNull(base.getAbsorbedBlock(), "kuculen kup blogunu birakmali");

        base.setAbsorbedBlock(BlockTypes.TNT.getDefaultState());
        assertNull(base.getAbsorbedBlock(), "kucuk kup blok ememez");
    }

    /**
     * Emilen blok kupun kisiligini belirliyor; hiz, sekme ve ozel etkiler hep buradan turuyor.
     */
    @Test
    void absorbedBlockDecidesTheArchetype() {
        assertEquals(SulfurCubeArchetype.EXPLOSIVE, SulfurCubeArchetypes.of(BlockTypes.TNT));
        assertEquals(SulfurCubeArchetype.HOT, SulfurCubeArchetypes.of(BlockTypes.MAGMA));
        assertEquals(SulfurCubeArchetype.STICKY, SulfurCubeArchetypes.of(BlockTypes.HONEYCOMB_BLOCK));
        assertEquals(SulfurCubeArchetype.HIGH_RESISTANCE, SulfurCubeArchetypes.of(BlockTypes.SOUL_SAND));
        assertEquals(SulfurCubeArchetype.FAST_SLIDING, SulfurCubeArchetypes.of(BlockTypes.BLUE_ICE));
        assertEquals(SulfurCubeArchetype.BOUNCY, SulfurCubeArchetypes.of(BlockTypes.OAK_PLANKS));
        assertEquals(SulfurCubeArchetype.LIGHT, SulfurCubeArchetypes.of(BlockTypes.WHITE_WOOL));

        // Listede olmayan blok emilemez; wiki yalnizca belirli aileleri sayiyor.
        assertNull(SulfurCubeArchetypes.of(BlockTypes.AIR), "hava emilebilir gorunuyor");
    }

    /**
     * Yalnizca TNT tasiyan kup ateslenebilir, ve yanan kupe artik dokunulamaz.
     */
    @Test
    void onlyExplosiveCubesCanBeIgnited() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);

        base.ignite(EntitySulfurCubeBaseComponentImpl.MANUAL_FUSE_TICKS);
        assertFalse(base.isIgnited(), "bos kup ateslenmemeli");

        base.setAbsorbedBlock(BlockTypes.OAK_PLANKS.getDefaultState());
        base.ignite(EntitySulfurCubeBaseComponentImpl.MANUAL_FUSE_TICKS);
        assertFalse(base.isIgnited(), "TNT tasimayan kup ateslenmemeli");

        base.setAbsorbedBlock(BlockTypes.TNT.getDefaultState());
        base.ignite(EntitySulfurCubeBaseComponentImpl.MANUAL_FUSE_TICKS);
        assertTrue(base.isIgnited(), "TNT tasiyan kup ateslenmeli");
    }

    /**
     * Oyuncunun uzattigi blogu kupun yutmasi.
     *
     * <p>Bu, oyunda hicbir sey olmadigi bildirilen yol. Testin isi, hatanin bu metodun icinde mi
     * yoksa daha oncesinde — yani istemcinin etkilesim paketini hic gondermemesinde mi — oldugunu
     * ayirmak.</p>
     */
    @Test
    void offeringABlockMakesTheCubeAbsorbIt() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);

        var player = mock(EntityPlayer.class);
        var planks = ItemTypes.OAK_PLANKS.createItemStack();

        assertTrue(cube.onInteract(player, planks), "kup uzatilan blogu kabul etmedi");
        assertNotNull(base.getAbsorbedBlock(), "blok emilmedi");
        assertEquals(BlockTypes.OAK_PLANKS, base.getAbsorbedBlock().getBlockType());
        assertEquals(SulfurCubeArchetype.BOUNCY, base.getArchetype());
    }

    /**
     * Makasla blogu geri alma. Oyunda calismadigi bildirildi; test bunun sunucu mantiginda mi yoksa
     * etkilesim paketinin hic gelmemesinde mi oldugunu ayiriyor.
     */
    @Test
    void shearsTakeTheBlockBack() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        base.setAbsorbedBlock(BlockTypes.OAK_PLANKS.getDefaultState());

        assertTrue(cube.onInteract(mock(EntityPlayer.class), ItemTypes.SHEARS.createItemStack()),
                "makas etkilesimi kabul edilmedi");
        assertNull(base.getAbsorbedBlock(), "blok makasla cikarilmadi");
        assertEquals(SulfurCubeArchetype.NONE,
                cube.getPropertyValue(EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE),
                "blok cikinca kisilik NONE'a donmeli");
    }

    /**
     * Blok tasiyan kupa vurmak hasar gecirmemeli — ve vurus "basarili" bile sayilmamali, yoksa
     * istemci kupu kirmizi yakip vurus sesi cikariyor. Oysa kup hicbir sey hissetmemis olmali.
     */
    @Test
    void hittingAnAbsorbedCubeDoesNotCount() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        var living = assertInstanceOf(EntityLivingComponent.class, cube);

        base.setAbsorbedBlock(BlockTypes.OAK_PLANKS.getDefaultState());
        var healthBefore = living.getHealth();

        assertFalse(living.attack(DamageContainer.entityAttack(mock(EntityPlayer.class), 5)),
                "savusturulan vurus basarili sayildi; kup kirmizi yanip ses cikarir");
        assertEquals(healthBefore, living.getHealth(), "blok tasiyan kup hasar aldi");
    }

    /**
     * Emilemeyen bir blok uzatilirsa kup onu reddetmeli; yoksa her sey yutulur hale gelir.
     */
    @Test
    void nonAbsorbableBlocksAreRefused() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);

        assertFalse(cube.onInteract(mock(EntityPlayer.class), ItemTypes.STICK.createItemStack()),
                "kup emilemeyen bir esyayi kabul etti");
        assertNull(base.getAbsorbedBlock());
    }

    /**
     * Wiki: "When a sulfur cube has absorbed a block, it stops moving."
     *
     * <p>Bu kural evaluator'a birakilamaz: davranis grubu bir davranisin evaluator'una yalnizca
     * baslarken bakiyor, calisirken degil. Dolasma davranisi suresiz oldugu icin bir kez
     * basladiginda kendiliginden hic bitmiyordu ve kup blogu yuttugu halde ziplamaya devam
     * ediyordu. Kosul o yuzden executor'un icinde.
     */
    @Test
    void anAbsorbedCubeStopsRoaming() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        var intelligent = assertInstanceOf(EntityIntelligent.class, cube);
        var roam = new SulfurCubeRoamExecutor(0.1f, 8, 120, false, -1, true, 10);

        base.setAbsorbedBlock(BlockTypes.OAK_PLANKS.getDefaultState());
        assertFalse(roam.execute(intelligent), "blok emmis kup hala dolasiyor");

        base.setAbsorbedBlock(null);
        assertTrue(roam.execute(intelligent), "blogu alinan kup yeniden dolasabilmeli");
    }

    /**
     * Kupun icinin nasil gorunecegi bu property'den okunuyor, ve istemci degeri <em>sirasina</em>
     * gore cozuyor. Yani sabitlerin arasina bir yenisi sokusturulursa ya da sira degisirse kup
     * yanlis seyi gostermeye baslar; derleme bunu yakalamaz. Beklenen sira vanilla'nin kendi
     * listesi.
     */
    @Test
    void archetypePropertyMatchesVanillaOrder() {
        var expected = new String[]{
                "none", "bouncy", "regular", "slow_bouncy", "slow_flat", "fast_flat", "light",
                "fast_sliding", "slow_sliding", "sticky", "high_resistance", "explosive", "hot"};

        assertArrayEquals(expected, EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE.serializedValues(),
                "arsetip sirasi vanilla ile uyusmuyor");
        assertEquals("minecraft:sulfur_cube_archetype", EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE.getName());
    }

    /**
     * Property varlik turune kaydedilmezse istemciye sema hic gonderilmez ve kup her zaman bos
     * gorunur. Kayit tek satir oldugu icin gozden kacmasi kolay.
     */
    @Test
    void sulfurCubeTypeDeclaresTheArchetypeProperty() {
        assertTrue(EntityTypes.SULFUR_CUBE.getProperties().containsKey(
                        EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE.getName()),
                "sulfur kupu arsetip property'sini tanimlamiyor");
    }

    /**
     * Blok emildiginde property de guncellenmelidir; kupun ici yalnizca bu deger uzerinden
     * degisiyor.
     */
    @Test
    void absorbingABlockUpdatesTheArchetypeProperty() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);

        assertEquals(SulfurCubeArchetype.NONE,
                cube.getPropertyValue(EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE),
                "bos kupun kisiligi NONE olmali");

        base.setAbsorbedBlock(BlockTypes.TNT.getDefaultState());
        assertEquals(SulfurCubeArchetype.EXPLOSIVE,
                cube.getPropertyValue(EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE),
                "emilen blok property'ye yansimadi");

        base.setAbsorbedBlock(null);
        assertEquals(SulfurCubeArchetype.NONE,
                cube.getPropertyValue(EntityPropertyTypes.SULFUR_CUBE_ARCHETYPE),
                "blok cikarilinca kisilik NONE'a donmeli");
    }

    /**
     * Zincirleme patlamanin fitili elle ateslenene gore cok daha kisa; bir kup yiginin pesi sira
     * patlamasini saglayan sey bu.
     */
    @Test
    void chainFuseIsShorterThanAManualOne() {
        for (int i = 0; i < 50; i++) {
            var fuse = EntitySulfurCubeBaseComponentImpl.randomChainFuse();
            assertTrue(fuse >= EntitySulfurCubeBaseComponentImpl.CHAIN_FUSE_MIN_TICKS
                       && fuse <= EntitySulfurCubeBaseComponentImpl.CHAIN_FUSE_MAX_TICKS,
                    "zincir fitili beklenen araligin disinda: " + fuse);
            assertTrue(fuse < EntitySulfurCubeBaseComponentImpl.MANUAL_FUSE_TICKS,
                    "zincir fitili elle ateslenenden kisa olmali");
        }
    }

    /**
     * Yutulan blok kupun <em>elindeki esya</em> olarak ciziliyor.
     *
     * <p>Resmi kaynak paketinde kisilik property'si sulfur cekirdegini tamamen gizliyor
     * ({@code part_visibility}) ve bosalan yeri dolduran tek sey kupun tuttugu esya
     * ({@code held_item_scale: 2.678}). Yalnizca property gonderildiginde kup cekirdegini kaybeder
     * ama yerine bir sey koymaz; oyunda "ici bombos gorunuyor" diye bildirilen hata tam olarak
     * buydu.</p>
     */
    @Test
    void absorbedBlockIsShownAsTheCubesHeldItem() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        var viewer = mock(WorldViewer.class);
        cube.spawnTo(viewer);

        base.setAbsorbedBlock(BlockTypes.GRASS_BLOCK.getDefaultState());

        var sent = ArgumentCaptor.forClass(ItemStack.class);
        verify(viewer, atLeastOnce()).viewEntityHandItem(eq(cube), sent.capture());
        assertEquals(BlockTypes.GRASS_BLOCK.getItemType(), sent.getValue().getItemType(),
                "yutulan blok kupun elinde gorunmuyor");

        base.setAbsorbedBlock(null);
        verify(viewer, atLeastOnce()).viewEntityHandItem(eq(cube), sent.capture());
        assertEquals(ItemTypes.AIR, sent.getValue().getItemType(),
                "blok alininca kupun eli bosalmali");
    }

    /**
     * Blok dogurma paketinde tasinmadigi icin, kupu sonradan goren bir oyuncuya ayrica
     * gonderilmeli; yoksa kup yalnizca yutma anini goren oyunculara dolu gorunur.
     */
    @Test
    void aLateViewerAlsoReceivesTheAbsorbedBlock() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        base.setAbsorbedBlock(BlockTypes.GRASS_BLOCK.getDefaultState());

        var viewer = mock(WorldViewer.class);
        cube.spawnTo(viewer);

        var sent = ArgumentCaptor.forClass(ItemStack.class);
        verify(viewer).viewEntityHandItem(eq(cube), sent.capture());
        assertEquals(BlockTypes.GRASS_BLOCK.getItemType(), sent.getValue().getItemType(),
                "kupu sonradan goren oyuncu icini bos goruyor");
    }

    /**
     * Blok-kisilik eslemesi oyunun kendi verisinden okunuyor.
     *
     * <p>Once bu esleme elle yazilmisti ve bloklari kimlik sonekleri ile blok etiketlerinden
     * tahmin ediyordu. Asagidaki uc blok hicbir kaliba uymadigi icin o listede hicbir kisilige denk
     * gelmiyor, yani emilemiyordu: islak sungerin kuru olani listedeydi ama kendisi degildi,
     * kalsit ve obsidyen ise tas ailesinin ne adini ne de etiketini tasiyor. Elle yazilmis bir
     * listeye geri donulurse test bunu hemen yakalar.</p>
     */
    @Test
    void archetypeMappingComesFromGameData() {
        assertEquals(SulfurCubeArchetype.FAST_FLAT, SulfurCubeArchetypes.of(BlockTypes.WET_SPONGE));
        assertEquals(SulfurCubeArchetype.SLOW_BOUNCY, SulfurCubeArchetypes.of(BlockTypes.CALCITE));
        assertEquals(SulfurCubeArchetype.SLOW_BOUNCY, SulfurCubeArchetypes.of(BlockTypes.OBSIDIAN));

        // Oyunda denenen blok; kisiligi tas degil toprak ailesinden gelmeli.
        assertEquals(SulfurCubeArchetype.REGULAR, SulfurCubeArchetypes.of(BlockTypes.GRASS_BLOCK));
        assertEquals(SulfurCubeArchetype.SLOW_FLAT, SulfurCubeArchetypes.of(BlockTypes.COPPER_BULB));
        assertEquals(SulfurCubeArchetype.SLOW_SLIDING, SulfurCubeArchetypes.of(BlockTypes.SHROOMLIGHT));
    }

    /**
     * Surtunme ve hava direnci motorun varsayilanina uygulanan birer <em>carpan</em>.
     *
     * <p>Once bunlar mutlak deger sanilip motora dogrudan veriliyordu ve kisilikler birbirinden
     * ayirt edilemiyordu: buzun {@code 0.05}'i motorun yerdeki {@code 0.09} varsayilaninin zaten
     * cok yakinina dustugu icin kup kaymiyordu. Carpan olarak uygulandiginda ayni deger yirmi kat
     * daha kaygan bir zemin veriyor. Testin esitlikleri tam da bu farki yakaliyor.</p>
     */
    @Test
    void frictionAndAirDragAreMultipliersOfTheEngineDefaults() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);
        var physics = assertInstanceOf(EntityPhysicsComponent.class, cube);

        // Blok tasimayan kup motorun varsayilanlarini kullanir; olculecek taban bu.
        var defaultGround = physics.getDragFactorOnGround();
        var defaultAir = physics.getDragFactorInAir();

        base.setAbsorbedBlock(BlockTypes.BLUE_ICE.getDefaultState());
        assertEquals(defaultGround * 0.05, physics.getDragFactorOnGround(), 1e-9,
                "buz tasiyan kup kaymiyor");

        base.setAbsorbedBlock(BlockTypes.HONEYCOMB_BLOCK.getDefaultState());
        assertEquals(defaultGround * 2.0, physics.getDragFactorOnGround(), 1e-9,
                "petek tasiyan kup yere yapismiyor");

        base.setAbsorbedBlock(BlockTypes.WHITE_WOOL.getDefaultState());
        assertEquals(defaultAir * 1.8, physics.getDragFactorInAir(), 1e-9,
                "yun tasiyan kup havada yavaslamiyor");

        // Yuzme ayri bir ozellik; sekme degerinden turetilemez. Yun (LIGHT) yuzer, buz (FAST_SLIDING) batar.
        assertTrue(physics.getWaterBuoyancy() > physics.getGravity(),
                "yuzen kisilikteki kup batiyor");

        base.setAbsorbedBlock(BlockTypes.BLUE_ICE.getDefaultState());
        assertTrue(physics.getWaterBuoyancy() < physics.getGravity(),
                "yuzmemesi gereken kisilikteki kup suyun ustunde duruyor");
    }

    /**
     * Blogu cikarilan kup onu aninda geri yutmamali.
     *
     * <p>Oyunda blok kopyalanmasina yol aciyordu: makasla cikarilan blok yere dusuyor, kup daha
     * oyuncu egilip almadan geri yutuyor, ikisi de ayni esyayi almis oluyordu. Resmi davranis
     * paketi bunu bes saniyelik bir zamanlayiciyla cozuyor.</p>
     */
    @Test
    void aCubeCannotInstantlyReabsorbTheBlockItJustLost() {
        var cube = EntityTypes.SULFUR_CUBE.createEntity(EntityInitInfo.builder().build());
        var base = assertInstanceOf(EntitySulfurCubeBaseComponent.class, cube);

        base.setAbsorbedBlock(BlockTypes.GRASS_BLOCK.getDefaultState());
        assertFalse(base.isPickupOnCooldown(), "blok yutmak beklemeyi baslatmamali");

        base.setAbsorbedBlock(null);
        assertTrue(base.isPickupOnCooldown(), "blogu cikan kup aninda yeni blok alabiliyor");

        assertEquals(20 * 5, EntitySulfurCubeBaseComponentImpl.PICKUP_TIMEOUT_TICKS,
                "resmi zamanlayici bes saniye");
    }
}
