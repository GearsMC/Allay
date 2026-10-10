package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityArrow;
import org.allaymc.api.entity.interfaces.EntityEvocationFang;
import org.allaymc.api.entity.interfaces.EntityFireball;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.data.PotionType;
import org.allaymc.server.entity.ai.sensor.NearestMonsterSensor;
import org.allaymc.server.entity.component.mob.EntityGuardianBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.allaymc.testutils.MobTestSite;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.joml.Vector3d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.allaymc.testutils.MobTestSite.waitFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AllayTestExtension.class)
class NewMobExecutorsWorldTest {

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
    void evokerFangsBiteTheTarget() {
        var evoker = site.spawn(EntityTypes.EVOCATION_ILLAGER, 0, 0, true);
        var pig = site.spawn(EntityTypes.PIG, 6, 0, true);
        var health = pig.getHealth();

        var executor = new EvokerSpellExecutor(MemoryTypes.ATTACK_TARGET, EvokerSpellExecutor.Spell.FANGS);
        site.onWorldThread(() -> executor.castFangs(evoker, pig));

        assertTrue(waitFor(() -> site.count(EntityEvocationFang.class, 24) > 0, 2000), "dis cizgisi dogmadi");
        assertTrue(waitFor(() -> pig.getHealth() < health, 5000), "disler hedefi isirmadi");
        assertEquals(health - 6, pig.getHealth(), 0.01);
        assertTrue(waitFor(() -> site.count(EntityEvocationFang.class, 24) == 0, 5000), "disler omrunu doldurunca kaybolmali");
    }

    @Test
    void ghastShootsALargeFireballOwnedByIt() {
        var ghast = site.spawn(EntityTypes.GHAST, 0, 0, true);
        var pig = site.spawn(EntityTypes.PIG, 16, 0, true);
        var executor = new GhastFireballAttackExecutor(MemoryTypes.ATTACK_TARGET, 0.08f, 64, 20, 8, true, 10, 50, 64);

        site.onWorldThread(() -> executor.shoot(ghast, pig));

        assertTrue(waitFor(() -> site.dimension().getEntities().values().stream()
                .anyMatch(e -> e instanceof EntityFireball fireball && fireball.getShooter() == ghast), 2000));
    }

    @Test
    void guardianLaserChargesThenHurtsTheTarget() {
        var guardian = site.spawn(EntityTypes.GUARDIAN, 0, 0, true);
        var pig = site.spawn(EntityTypes.PIG, 6, 0, true);
        var health = pig.getHealth();
        guardian.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, pig.getRuntimeId());
        var base = (EntityGuardianBaseComponentImpl) ((EntityImpl) guardian).getBaseComponent();

        var executor = new GuardianLaserExecutor(MemoryTypes.ATTACK_TARGET, 15, true, 0, 40, 5);
        site.onWorldThread(() -> {
            executor.onStart(guardian);
            executor.execute(guardian);
            executor.execute(guardian);
        });
        assertTrue(base.isFiringLaser(), "sarj sirasinda lazer hedefi yayinlanmali");
        var metadata = new EntityDataMap();
        base.writeMetadata(metadata);
        assertEquals(pig.getUniqueId().getLeastSignificantBits(), (long) metadata.get(EntityDataTypes.TARGET_EID));
        assertEquals(health, pig.getHealth(), 0.01, "sarj bitmeden hasar olmamali");

        site.onWorldThread(() -> {
            for (var i = 0; i < 40 && pig.getHealth() >= health; i++) {
                executor.execute(guardian);
            }
        });
        assertTrue(pig.getHealth() < health, "lazer sarji bitince hedef hasar almali");
        assertFalse(base.isFiringLaser(), "atistan sonra lazer kapanmali");
    }

    @Test
    void wardenSonicBoomHurtsThroughDistance() {
        var warden = site.spawn(EntityTypes.WARDEN, 0, 0, true);
        var pig = site.spawn(EntityTypes.PIG, 10, 0, true);
        var health = pig.getHealth();

        site.onWorldThread(() -> new WardenSonicBoomExecutor(MemoryTypes.ATTACK_TARGET).boom(warden, pig));

        assertEquals(Math.max(0, health - 10), pig.getHealth(), 0.01);
    }

    @Test
    void snowGolemSeesNearbyMonstersButNotAnimals() {
        var golem = site.spawn(EntityTypes.SNOW_GOLEM, 0, 0, true);
        site.spawn(EntityTypes.PIG, 3, 0, true);
        var sensor = new NearestMonsterSensor(10, 1);

        site.onWorldThread(() -> sensor.sense(golem));
        assertNull(golem.getMemoryStorage().get(NearestMonsterSensor.NEAREST_MONSTER), "domuz canavar sayilmamali");

        var husk = site.spawn(EntityTypes.HUSK, 5, 0, true);
        site.onWorldThread(() -> sensor.sense(golem));
        assertEquals(husk.getRuntimeId(), golem.getMemoryStorage().get(NearestMonsterSensor.NEAREST_MONSTER));
    }

    @Test
    void tippedArrowsApplyAnEighthOfThePotionDuration() {
        var pig = site.spawn(EntityTypes.PIG, 4, 0, true);
        var arrow = (EntityArrow) EntityTypes.ARROW.createEntity(EntityInitInfo.builder()
                .dimension(site.dimension())
                .pos(site.at(1.5, 0.5, 0))
                .motion(new Vector3d(1.5, 0, 0))
                .build());
        arrow.setPotionType(PotionType.SLOWNESS);
        site.track(arrow);
        site.dimension().getEntityManager().addEntity(arrow);

        assertTrue(waitFor(() -> pig.hasEffect(EffectTypes.SLOWNESS), 3000), "ok domuza isabet etmedi");
        var duration = pig.getEffects().get(EffectTypes.SLOWNESS).getDuration();
        assertTrue(duration <= 90 * 20 / 8, "uclu ok iksirin sekizde biri kadar surmeli: " + duration);
    }
}
