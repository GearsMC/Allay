package org.allaymc.server.entity.component.projectile;

import org.allaymc.api.block.dto.Block;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class EntityLlamaSpitPhysicsComponentImpl extends EntityProjectilePhysicsComponentImpl {

    protected static final float DAMAGE = 1;

    @Override
    public double getGravity() {
        return 0.06;
    }

    @Override
    protected void onHitEntity(Entity other, Vector3dc hitPos) {
        if (thisEntity.willBeDespawnedLater()) {
            return;
        }

        if (other instanceof EntityLiving living) {
            var damage = DamageContainer.projectile(thisEntity, DAMAGE);
            damage.setHasKnockback(false);
            damage.setKnockbackSource(hitPos.sub(this.motion, new Vector3d()));
            living.attack(damage);
        }

        thisEntity.remove();
    }

    @Override
    protected void onHitBlock(Block block, Vector3dc hitPos) {
        if (thisEntity.willBeDespawnedLater()) {
            return;
        }

        thisEntity.remove();
    }
}
