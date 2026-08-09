package org.allaymc.server.item.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.item.component.ItemRepairableComponent;
import org.allaymc.api.item.interfaces.ItemSpearStack;
import org.allaymc.server.component.ComponentProvider;
import org.allaymc.server.item.SpearJab;

import java.util.List;

/**
 * A spear.
 * <p>
 * Both of a spear's attacks are resolved by the server rather than the client, so the
 * hooks below are what make it a weapon at all; see {@link SpearJab}. Holding use starts
 * a charge, and the per-tick hook drives it.
 */
public class ItemSpearStackImpl extends ItemStackImpl implements ItemSpearStack {
    @Delegate
    private ItemRepairableComponent repairableComponent;

    public ItemSpearStackImpl(ItemStackInitInfo initInfo,
                              List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }

    @Override
    public boolean canUseItemInAir(EntityPlayer player) {
        // Holding use charges the spear, so it must be a "takes time to use" item.
        // A spear still recovering from a jab cannot start a charge.
        if (!player.isCooldownEnd(getItemType())) {
            return false;
        }
        SpearJab.playUseSound(player, this);
        return true;
    }

    @Override
    public void onUseInAirTick(EntityPlayer player, long usedTime) {
        SpearJab.sampleMovement(player);
        SpearJab.tickCharge(player, this, usedTime);
    }

    @Override
    public boolean useItemInAir(EntityPlayer player, long usingTime) {
        SpearJab.clearCharge(player);
        return false;
    }
}
