package org.allaymc.server.entity.component;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Buradaki illager'larin — pillager ve vindicator — paylastigi canli varlik bileseni.
 *
 * <p>Ikisinin de cani ve deneyimi ayni, ve ikisi de yalnizca tasidiklari silahi, o da nadiren
 * dusurur. Dusurme sansi bilerek vanilla seviyesinde tutuldu: bu moblar GearsCore mob
 * bloklarindan dalgalar halinde cikiyor, garanti bir dusurme sunucuyu arbalet ve demir baltayla
 * doldururdu.</p>
 */
public class EntityIllagerLivingComponentImpl extends EntityHostileLivingComponentImpl {

    protected static final float WEAPON_DROP_CHANCE = 0.085f;

    public EntityIllagerLivingComponentImpl() {
        setMaxHealth(24);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drops = new ArrayList<ItemStack>();
        if (containerHolderComponent == null) {
            return drops;
        }

        var rand = ThreadLocalRandom.current();
        if (rand.nextFloat() >= WEAPON_DROP_CHANCE + (0.01f * lootingLevel)) {
            return drops;
        }

        var weapon = containerHolderComponent.getContainer(ContainerTypes.ENTITY_HAND).getItemInHand();
        if (weapon.getItemType() != ItemTypes.AIR) {
            drops.add(weapon.copy());
        }

        return drops;
    }

    @Override
    public int getDropXpAmount() {
        return 5;
    }
}
