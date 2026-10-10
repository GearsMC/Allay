package org.allaymc.server.entity.effect;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.EntityState;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityZombie;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.server.Server;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AllayTestExtension.class)
public class EffectReplaceTest {

    private static EntityZombie zombie() {
        var entity = EntityTypes.ZOMBIE.createEntity(EntityInitInfo.builder()
                .dimension(Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension())
                .pos(0, 64, 0).build());
        var base = (EntityBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
        base.setState(EntityState.SPAWNED_LATER);
        base.setState(EntityState.ALIVE);
        return entity;
    }

    @Test
    void healthBoostUpgradeAppliesNewLevelAndRestoresBaseOnExpiry() {
        var entity = zombie();
        var base = entity.getMaxHealth();

        assertTrue(entity.addEffect(EffectTypes.HEALTH_BOOST.createInstance(0, 600)));
        assertEquals(base + 4, entity.getMaxHealth(), 0.001f);
        entity.setHealth(entity.getMaxHealth());

        assertTrue(entity.addEffect(EffectTypes.HEALTH_BOOST.createInstance(1, 600)));
        assertEquals(base + 8, entity.getMaxHealth(), 0.001f);
        assertEquals(base + 4, entity.getHealth(), 0.001f);

        entity.removeEffect(EffectTypes.HEALTH_BOOST);
        assertEquals(base, entity.getMaxHealth(), 0.001f);
        assertEquals(base, entity.getHealth(), 0.001f);
    }

    @Test
    void healthBoostRefreshAtSameLevelKeepsHealthAndMaxHealth() {
        var entity = zombie();
        var base = entity.getMaxHealth();

        assertTrue(entity.addEffect(EffectTypes.HEALTH_BOOST.createInstance(1, 600)));
        entity.setHealth(entity.getMaxHealth());

        for (int i = 0; i < 5; i++) {
            assertTrue(entity.addEffect(EffectTypes.HEALTH_BOOST.createInstance(1, 600)));
            assertEquals(base + 8, entity.getMaxHealth(), 0.001f);
            assertEquals(base + 8, entity.getHealth(), 0.001f);
        }

        entity.removeEffect(EffectTypes.HEALTH_BOOST);
        assertEquals(base, entity.getMaxHealth(), 0.001f);
        assertEquals(base, entity.getHealth(), 0.001f);
    }

    @Test
    void rejectedWeakerHealthBoostLeavesMaxHealthUntouched() {
        var entity = zombie();
        var base = entity.getMaxHealth();

        assertTrue(entity.addEffect(EffectTypes.HEALTH_BOOST.createInstance(2, 600)));
        assertFalse(entity.addEffect(EffectTypes.HEALTH_BOOST.createInstance(0, 6000)));
        assertEquals(base + 12, entity.getMaxHealth(), 0.001f);

        entity.removeEffect(EffectTypes.HEALTH_BOOST);
        assertEquals(base, entity.getMaxHealth(), 0.001f);
    }

    @Test
    void absorptionRefillsOnSameLevelAndUpgrades() {
        var entity = zombie();

        assertTrue(entity.addEffect(EffectTypes.ABSORPTION.createInstance(0, 2400)));
        assertEquals(4, entity.getAbsorption(), 0.001f);

        entity.setAbsorption(1);
        assertTrue(entity.addEffect(EffectTypes.ABSORPTION.createInstance(0, 2400)));
        assertEquals(4, entity.getAbsorption(), 0.001f);

        assertTrue(entity.addEffect(EffectTypes.ABSORPTION.createInstance(1, 100)));
        assertEquals(8, entity.getAbsorption(), 0.001f);

        entity.removeEffect(EffectTypes.ABSORPTION);
        assertEquals(0, entity.getAbsorption(), 0.001f);
    }

    @Test
    void absorptionDoesNotLowerHigherExistingAbsorption() {
        var entity = zombie();
        entity.setAbsorption(10);

        assertTrue(entity.addEffect(EffectTypes.ABSORPTION.createInstance(0, 2400)));
        assertEquals(10, entity.getAbsorption(), 0.001f);
    }
}
