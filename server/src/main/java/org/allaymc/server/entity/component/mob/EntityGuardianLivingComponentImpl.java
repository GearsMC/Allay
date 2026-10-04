package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityProjectile;
import org.allaymc.server.component.annotation.Dependency;

import java.util.function.ToIntFunction;

public class EntityGuardianLivingComponentImpl extends EntityMobLivingComponentImpl {

    protected static final float SPIKE_DAMAGE = 2;

    @Dependency
    protected EntityGuardianBaseComponentImpl guardianBase;

    public EntityGuardianLivingComponentImpl(float maxHealth, MobLoot loot, ToIntFunction<LootContext> xp) {
        super(maxHealth, loot, xp);
        breathing(Breathing.AMPHIBIOUS);
    }

    @Override
    public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
        if (!super.attack(damage, ignoreCoolDown)) {
            return false;
        }

        if (damage.getDamageType() == DamageType.ENTITY_ATTACK
                && !(damage.getAttacker() instanceof EntityProjectile)
                && damage.getAttacker() instanceof EntityLiving attacker
                && attacker != thisEntity
                && !guardianBase.isFiringLaser()) {
            attacker.attack(new DamageContainer(thisEntity, DamageType.THORNS, SPIKE_DAMAGE));
        }
        return true;
    }
}
