package org.allaymc.api.entity.damage;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.interfaces.EntityProjectile;
import org.allaymc.api.message.TrKeys;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mermi olum mesaji: olduren merminin atanidir; eklentiler sanal mermilerde saldirgani dogrudan
 * verebilir. Eskiden saldirgan her zaman mermiye cevriliyordu ve ClassCastException/NPE atiyordu.
 */
class ProjectileDeathInfoTest {

    private static EntityPlayer player(String name) {
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        Mockito.when(player.getDisplayName()).thenReturn(name);
        return player;
    }

    private final EntityPlayer victim = player("Kurban");

    @Test
    void arrowCreditsItsShooter() {
        EntityProjectile arrow = Mockito.mock(EntityProjectile.class);
        EntityPlayer shooter = player("Okcu");
        Mockito.when(arrow.getShooter()).thenReturn(shooter);

        var info = DamageType.PROJECTILE.getDeathInfo(victim, arrow);
        assertEquals(TrKeys.MC_DEATH_ATTACK_ARROW, info.left());
        assertArrayEquals(new String[] {"Kurban", "Okcu"}, info.right());
    }

    @Test
    void virtualProjectileCreditsPlayerAttackerDirectly() {
        // Or. GearsCore Real Knife kesmesi: varligi olmayan mermi, saldirgan oyuncunun kendisi.
        var info = DamageType.PROJECTILE.getDeathInfo(victim, player("Bicakci"));
        assertArrayEquals(new String[] {"Kurban", "Bicakci"}, info.right());
    }

    @Test
    void nonProjectileEntityAttackerUsesItsName() {
        // Or. GearsCore ejderha ates topu: saldirgan ejderhanin kendisi.
        Entity dragon = Mockito.mock(Entity.class);
        Mockito.when(dragon.getNameTag()).thenReturn(null);
        Mockito.when(dragon.getDisplayName()).thenReturn("Ender Ejderhasi");

        var info = DamageType.PROJECTILE.getDeathInfo(victim, dragon);
        assertArrayEquals(new String[] {"Kurban", "Ender Ejderhasi"}, info.right());
    }

    @Test
    void projectileWithoutShooterUsesProjectileItself() {
        EntityProjectile arrow = Mockito.mock(EntityProjectile.class);
        Mockito.when(arrow.getShooter()).thenReturn(null);
        Mockito.when(arrow.getNameTag()).thenReturn(null);
        Mockito.when(arrow.getDisplayName()).thenReturn("Ok");

        var info = DamageType.PROJECTILE.getDeathInfo(victim, arrow);
        assertArrayEquals(new String[] {"Kurban", "Ok"}, info.right());
    }
}
