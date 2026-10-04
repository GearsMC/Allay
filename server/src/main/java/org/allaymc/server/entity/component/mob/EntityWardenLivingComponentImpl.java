package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.damage.DamageType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.component.annotation.Dependency;

public class EntityWardenLivingComponentImpl extends EntityMobLivingComponentImpl {

    @Dependency
    protected EntityWardenBaseComponentImpl wardenBase;

    public EntityWardenLivingComponentImpl() {
        super(500, (context, drops) -> MobLoot.add(drops, ItemTypes.SCULK_CATALYST, 1), MobLoot.HOSTILE_XP);
        fireproof();
        immuneTo(DamageType.LAVA, DamageType.FIRE, DamageType.FIRE_TICK, DamageType.DROWN);
    }

    @Override
    public boolean attack(DamageContainer damage, boolean ignoreCoolDown) {
        if (!super.attack(damage, ignoreCoolDown)) {
            return false;
        }

        var attacker = resolveAttacker(damage.getAttacker());
        if (attacker != null) {
            wardenBase.addAnger(attacker, EntityWardenBaseComponentImpl.HIT_ANGER);
        }
        return true;
    }
}
