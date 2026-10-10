package org.allaymc.server.entity.effect;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.EntityState;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.interfaces.EntityZombie;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.player.Player;
import org.allaymc.api.server.Server;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.allaymc.server.player.PlayerMovementSpeed;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(AllayTestExtension.class)
public class EffectCalculationTest {

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
    void strengthAddsThirtyPercentOfBaseDamagePerLevel() {
        var attacker = zombie();
        attacker.addEffect(EffectTypes.STRENGTH.createInstance(1, 600));
        var victim = zombie();
        var health = victim.getHealth();
        assertTrue(victim.attack(DamageContainer.entityAttack(attacker, 5)));
        assertEquals(health - 8.0f, victim.getHealth(), 0.001f);
    }

    @Test
    void weaknessRemovesTwentyPercentOfBaseDamagePerLevel() {
        var attacker = zombie();
        attacker.addEffect(EffectTypes.WEAKNESS.createInstance(0, 600));
        var victim = zombie();
        var health = victim.getHealth();
        assertTrue(victim.attack(DamageContainer.entityAttack(attacker, 5)));
        assertEquals(health - 4.0f, victim.getHealth(), 0.001f);
    }

    @Test
    void weakerOrShorterEffectDoesNotReplaceActiveOne() {
        var entity = zombie();
        assertTrue(entity.addEffect(EffectTypes.SPEED.createInstance(1, 1000)));
        assertFalse(entity.addEffect(EffectTypes.SPEED.createInstance(1, 60)));
        assertFalse(entity.addEffect(EffectTypes.SPEED.createInstance(0, 5000)));
        assertEquals(1000, entity.getEffects().get(EffectTypes.SPEED).getDuration());

        assertTrue(entity.addEffect(EffectTypes.SPEED.createInstance(1, 1000)));
        assertTrue(entity.addEffect(EffectTypes.SPEED.createInstance(2, 20)));
        assertEquals(3, entity.getEffectLevel(EffectTypes.SPEED));
    }

    @Test
    void movementMultiplierMatchesPocketMine() {
        assertEquals(1.0, PlayerMovementSpeed.calculateMultiplier(false, 0, 0), 1e-9);
        assertEquals(1.4, PlayerMovementSpeed.calculateMultiplier(false, 2, 0), 1e-9);
        assertEquals(1.3 * 1.4, PlayerMovementSpeed.calculateMultiplier(true, 2, 0), 1e-9);
        assertEquals(0.85, PlayerMovementSpeed.calculateMultiplier(false, 0, 1), 1e-9);
        assertEquals(0.0, PlayerMovementSpeed.calculateMultiplier(false, 0, 7), 1e-9);
    }

    @Test
    void speedReturnsToDefaultAfterLevelChangesAndSprintToggles() {
        var speedLevel = new AtomicInteger();
        var sprinting = new AtomicReference<>(false);
        var speed = new AtomicReference<>(Player.DEFAULT_SPEED);

        var controller = mock(Player.class);
        when(controller.getSpeed()).thenAnswer(i -> speed.get());
        doAnswer(i -> {
            speed.set(i.getArgument(0));
            return null;
        }).when(controller).setSpeed(any());

        var entity = mock(EntityPlayer.class);
        when(entity.isActualPlayer()).thenReturn(true);
        when(entity.getController()).thenReturn(controller);
        when(entity.isSprinting()).thenAnswer(i -> sprinting.get());
        when(entity.getEffectLevel(EffectTypes.SPEED)).thenAnswer(i -> speedLevel.get());
        when(entity.getEffectLevel(EffectTypes.SLOWNESS)).thenReturn(0);

        speedLevel.set(1);
        PlayerMovementSpeed.update(entity);
        sprinting.set(true);
        PlayerMovementSpeed.update(entity);
        speedLevel.set(2);
        PlayerMovementSpeed.update(entity);
        assertEquals(0.1 * 1.3 * 1.4, speed.get().calculate(), 1e-9);

        speedLevel.set(0);
        PlayerMovementSpeed.update(entity);
        sprinting.set(false);
        PlayerMovementSpeed.update(entity);
        assertEquals(Player.DEFAULT_SPEED.calculate(), speed.get().calculate(), 1e-9);
    }
}
