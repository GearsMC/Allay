package org.allaymc.server.entity.component;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.interfaces.EntityProjectile;
import org.allaymc.server.component.annotation.Dependency;

/**
 * Kendisine vurana karsilik veren saldirgan moblar icin canli varlik bileseni.
 *
 * <p>Her saldirgan mobun ayni tepkiyi vermesi gerekiyor: bir oyuncu tarafindan yaralandiginda
 * onu {@link MemoryTypes#ATTACK_TARGET} icinde hatirlar, boylece davranis grubu mob kendi
 * halinde dolasirken bile onu kovalayabilir.</p>
 */
public class EntityHostileLivingComponentImpl extends EntityLivingComponentImpl {

    @Dependency
    protected EntityAIComponent aiComponent;

    /**
     * Bir seyden zarar gormenin kin tutmaya deger olup olmadigina karar verir.
     *
     * <p><strong>Yalnizca oyuncular sayilir.</strong> Moblar istemeden surekli birbirini
     * yaraliyor — kalabaligin ortasinda patlayan bir creeper, hedefini sasiran bir iskelet oku,
     * yanindaki piglin'e degen bir blaze ates topu — ve karsilik vermeleri her mob blok
     * dalgasini oyuncu daha gelmeden kendi kendini bitiren bir arbedeye ceviriyordu. Moblar
     * arasindaki hasar hala isliyor; sadece kin tutulmuyor.</p>
     *
     * <p>{@code ATTACK_TARGET}'i yazan tek yer burasi oldugu icin, bir mobun hedefinin her zaman
     * bir oyuncu oldugunu garanti eden sey de bu kisittir.</p>
     *
     * @param attacker hasari veren varlik; mermiler zaten aticiya cozulmus halde gelir
     * @return kurbanin saldirani avlamaya baslayip baslamayacagi
     */
    public static boolean isRetaliationTarget(Entity attacker) {
        return attacker instanceof EntityPlayer && attacker.isAlive();
    }

    /**
     * Bir vurustan gercekten sorumlu olan varligi bulur. Mermi, onu atanla degistirilir; yoksa
     * uzaktan vurulan bir mob okcunun degil okun ustune kosardi.
     *
     * @param attacker hasarin uzerinde kayitli olan ham saldiran
     * @return sorumlu varlik; hasar hicbir varliktan gelmiyorsa {@code null}
     */
    public static Entity resolveAttacker(Object attacker) {
        if (attacker instanceof EntityProjectile projectile) {
            return projectile.getShooter();
        }

        return attacker instanceof Entity entity ? entity : null;
    }

    @Override
    public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
        if (!super.attack(damage, ignoreCoolDown)) {
            return false;
        }

        var attacker = resolveAttacker(damage.getAttacker());
        if (isRetaliationTarget(attacker)) {
            aiComponent.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, attacker.getRuntimeId());
        }

        return true;
    }
}
