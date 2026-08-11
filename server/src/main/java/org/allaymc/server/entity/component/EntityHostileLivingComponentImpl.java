package org.allaymc.server.entity.component;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityProjectile;
import org.allaymc.server.component.annotation.Dependency;

/**
 * Living component for hostile mobs that retaliate against whoever hurt them.
 *
 * <p>Every hostile mob needs the same reaction: once damaged, remember the attacker in
 * {@link MemoryTypes#ATTACK_TARGET} so the behavior group can chase it even when the mob was
 * minding its own business. Projectiles are resolved back to their shooter, otherwise a mob
 * shot from a distance would run at the arrow instead of the archer.</p>
 */
public class EntityHostileLivingComponentImpl extends EntityLivingComponentImpl {

    @Dependency
    protected EntityAIComponent aiComponent;

    @Override
    public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
        if (!super.attack(damage, ignoreCoolDown)) {
            return false;
        }

        var attacker = resolveAttacker(damage.getAttacker());
        if (attacker == null || attacker == thisEntity || !attacker.isAlive() || !(attacker instanceof EntityLivingComponent)) {
            return true;
        }

        aiComponent.getMemoryStorage().put(MemoryTypes.ATTACK_TARGET, attacker.getRuntimeId());
        return true;
    }

    protected Entity resolveAttacker(Object attacker) {
        if (attacker instanceof EntityProjectile projectile) {
            return projectile.getShooter();
        }

        return attacker instanceof Entity entity ? entity : null;
    }
}
