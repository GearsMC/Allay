package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityZombie;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.entity.EntityDamageAttemptEvent;
import org.allaymc.api.eventbus.event.entity.EntityDamageEvent;
import org.allaymc.api.server.Server;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AllayTestExtension.class)
public class CombatDamageTest {
    private EntityZombie victim() {
        var entity = EntityTypes.ZOMBIE.createEntity(EntityInitInfo.builder()
                .dimension(Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension())
                .pos(0, 64, 0).build());
        var base = (org.allaymc.server.entity.component.EntityBaseComponentImpl)
                ((org.allaymc.server.entity.impl.EntityImpl) entity).getBaseComponent();
        base.setState(org.allaymc.api.entity.EntityState.SPAWNED_LATER);
        base.setState(org.allaymc.api.entity.EntityState.ALIVE);
        return entity;
    }

    public static final class CancelListener {
        final EntityZombie victim;
        CancelListener(EntityZombie victim) { this.victim = victim; }
        @EventHandler public void cancel(EntityDamageEvent event) {
            if (event.getEntity() == victim) event.cancel();
        }
    }

    public static final class AttemptListener {
        final EntityZombie victim;
        AttemptListener(EntityZombie victim) { this.victim = victim; }
        @EventHandler public void attempt(EntityDamageAttemptEvent event) {
            if (event.getEntity() == victim) event.setIgnoreCoolDown(true);
        }
    }

    @Test
    void cancelledHitDoesNotConsumeCooldownOrReplaceKiller() {
        var victim = victim();
        var listener = new CancelListener(victim);
        var bus = Server.getInstance().getEventBus();
        bus.registerListener(listener);
        try {
            assertFalse(victim.attack(DamageContainer.simpleAttack(2)));
            assertNull(victim.getLastDamage());
            assertEquals(-1, victim.getLastDamageTime());
        } finally { bus.unregisterListener(listener); }
        assertTrue(victim.attack(DamageContainer.simpleAttack(2)));
        var accepted = victim.getLastDamage();
        bus.registerListener(listener);
        try {
            assertFalse(victim.attack(DamageContainer.simpleAttack(3), true));
            assertSame(accepted, victim.getLastDamage());
        } finally { bus.unregisterListener(listener); }
    }

    @Test
    void ignoreReductionSkipsResistanceLikeFixedSkillDamage() {
        // Direnc II %40 indirir; sabit yetenek hasari (PM MODIFIER_RESISTANCE = 0) bunu atlamali.
        var reduced = victim();
        reduced.addEffect(EffectTypes.RESISTANCE.createInstance(1, 600));
        assertTrue(reduced.attack(DamageContainer.simpleAttack(10)));
        assertEquals(14.0f, reduced.getHealth(), 0.001f);

        var fixed = victim();
        fixed.addEffect(EffectTypes.RESISTANCE.createInstance(1, 600));
        var damage = DamageContainer.simpleAttack(10);
        damage.setIgnoreReduction(true);
        assertTrue(fixed.attack(damage));
        assertEquals(10.0f, fixed.getHealth(), 0.001f);
    }

    @Test
    void explicitMotionReplacesVelocityWithoutDoublingKnockback() {
        var victim = victim();
        victim.setMotion(new Vector3d(1, 2, 3));
        var damage = DamageContainer.simpleAttack(2);
        damage.setKnockbackMotion(new Vector3d(0.475, 0.44, 0));
        assertTrue(victim.attack(damage));
        assertEquals(new Vector3d(0.475, 0.44, 0), victim.getMotion());
    }

    @Test
    void attemptCanOverrideCooldownWithoutBypassingDamageProtection() {
        var victim = victim();
        assertTrue(victim.attack(DamageContainer.simpleAttack(1)));
        assertFalse(victim.attack(DamageContainer.simpleAttack(2)));
        var listener = new AttemptListener(victim);
        var bus = Server.getInstance().getEventBus();
        bus.registerListener(listener);
        try { assertTrue(victim.attack(DamageContainer.simpleAttack(2))); }
        finally { bus.unregisterListener(listener); }
    }
}
