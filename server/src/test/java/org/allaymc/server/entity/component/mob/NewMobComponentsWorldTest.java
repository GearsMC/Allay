package org.allaymc.server.entity.component.mob;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.container.interfaces.InventoryContainer;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityCow;
import org.allaymc.api.entity.interfaces.EntityFrog;
import org.allaymc.api.entity.interfaces.EntityHoglin;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.interfaces.EntityVillagerV2;
import org.allaymc.api.entity.interfaces.EntityZoglin;
import org.allaymc.api.entity.property.enums.ArmadilloState;
import org.allaymc.api.entity.property.type.EntityPropertyTypes;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.entity.impl.EntityImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.allaymc.testutils.MobTestSite;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.allaymc.testutils.MobTestSite.waitFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AllayTestExtension.class)
class NewMobComponentsWorldTest {

    private MobTestSite site;

    @BeforeEach
    void setUp() {
        site = MobTestSite.create();
    }

    @AfterEach
    void tearDown() {
        site.cleanUp();
    }

    @Test
    void hoglinTurnsIntoZoglinOutsideTheNether() {
        var hoglin = site.spawn(EntityTypes.HOGLIN, 0, 0, true);
        var base = base(hoglin);
        site.onWorldThread(() -> base.conversionTicks = base.conversionDelay - 2);

        assertTrue(waitFor(() -> site.findNear(EntityZoglin.class, 4) != null, 3000), "yerine zoglin dogmali");
        site.track(site.findNear(EntityZoglin.class, 4));
        assertTrue(waitFor(() -> site.count(EntityHoglin.class, 4) == 0, 2000), "donusen hoglin dunyadan kalkmali");
    }

    @Test
    void tadpoleGrowsIntoFrog() {
        var tadpole = site.spawn(EntityTypes.TADPOLE, 0, 0, true);
        var base = base(tadpole);
        site.onWorldThread(() -> base.conversionTicks = base.conversionDelay - 2);

        assertTrue(waitFor(() -> site.findNear(EntityFrog.class, 4) != null, 3000), "iribas kurbagaya donusmedi");
        site.track(site.findNear(EntityFrog.class, 4));
    }

    @Test
    void shearingMooshroomLeavesCowAndFiveMushrooms() {
        var mooshroom = site.spawn(EntityTypes.MOOSHROOM, 0, 0, true);
        var player = mockPlayer();
        var shears = ItemTypes.SHEARS.createItemStack(1);

        site.onWorldThread(() -> assertTrue(mooshroom.onInteract(player, shears)));

        assertTrue(waitFor(() -> site.findNear(EntityCow.class, 4) != null, 2000), "makaslanan mooshroom inege donmeli");
        site.track(site.findNear(EntityCow.class, 4));
        assertTrue(waitFor(() -> site.dimension().getEntities().values().stream()
                .anyMatch(e -> e instanceof EntityItem item
                               && item.getItemStack().getItemType() == ItemTypes.RED_MUSHROOM
                               && item.getItemStack().getCount() == 5), 2000), "5 mantar dusmeli");
        assertTrue(shears.getDamage() > 0, "makas asinmali");
    }

    @Test
    void curingZombieVillagerTurnsItIntoVillager() {
        var zombieVillager = site.spawn(EntityTypes.ZOMBIE_VILLAGER_V2, 0, 0, true);
        var base = (EntityZombieVillagerBaseComponentImpl) base(zombieVillager);
        var player = mockPlayer();

        site.onWorldThread(() -> assertFalse(zombieVillager.onInteract(player, ItemTypes.GOLDEN_APPLE.createItemStack(1)),
                "zayiflik olmadan altin elma iyilestirmez"));

        site.onWorldThread(() -> {
            zombieVillager.addEffect(new EffectInstance(EffectTypes.WEAKNESS, 0, 600, false, true));
            assertTrue(zombieVillager.onInteract(player, ItemTypes.GOLDEN_APPLE.createItemStack(1)));
        });
        assertTrue(base.isCuring());
        var metadata = new EntityDataMap();
        base.writeMetadata(metadata);
        assertTrue(Boolean.TRUE.equals(metadata.getFlags().get(EntityFlag.SHAKING)), "iyilesen zombi koylu titremeli");
        assertTrue(base.conversionDelay >= 3600 && base.conversionDelay <= 6000);

        site.onWorldThread(() -> base.conversionTicks = base.conversionDelay - 2);
        assertTrue(waitFor(() -> site.findNear(EntityVillagerV2.class, 4) != null, 3000), "koyluye donusmedi");
        site.track(site.findNear(EntityVillagerV2.class, 4));
    }

    @Test
    void armadilloRollsUpWhenHurtAndStaysStill() {
        var armadillo = site.spawn(EntityTypes.ARMADILLO, 0, 0, false);
        assertEquals(ArmadilloState.UNROLLED, armadillo.getPropertyValue(EntityPropertyTypes.ARMADILLO_STATE));

        site.onWorldThread(() -> armadillo.attack(DamageContainer.simpleAttack(1)));

        assertTrue(waitFor(() -> armadillo.getPropertyValue(EntityPropertyTypes.ARMADILLO_STATE) == ArmadilloState.ROLLED_UP, 2000));
        assertTrue(((EntityArmadilloBaseComponentImpl) base(armadillo)).isRolled());
    }

    @Test
    void goatGivesMilk() {
        var goat = site.spawn(EntityTypes.GOAT, 0, 0, true);
        var player = mockPlayer();

        site.onWorldThread(() -> assertTrue(goat.onInteract(player, ItemTypes.BUCKET.createItemStack(1))));

        verify(player).tryAddItem(argThat(item -> item.getItemType() == ItemTypes.MILK_BUCKET));
    }

    @Test
    void boggedCanBeShearedOnce() {
        var bogged = site.spawn(EntityTypes.BOGGED, 0, 0, true);
        var base = (EntityBoggedBaseComponentImpl) ((EntityImpl) bogged).getBaseComponent();
        var player = mockPlayer();

        site.onWorldThread(() -> assertTrue(bogged.onInteract(player, ItemTypes.SHEARS.createItemStack(1))));
        assertTrue(base.isSheared());
        site.onWorldThread(() -> assertFalse(bogged.onInteract(player, ItemTypes.SHEARS.createItemStack(1)),
                "ikinci kez makaslanmamali"));
    }

    private static EntityPlayer mockPlayer() {
        var player = mock(EntityPlayer.class);
        var inventory = mock(InventoryContainer.class);
        when(player.getContainer(ContainerTypes.INVENTORY)).thenReturn(inventory);
        return player;
    }

    private static EntityMobBaseComponentImpl base(Object entity) {
        return (EntityMobBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
    }
}
