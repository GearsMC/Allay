package org.allaymc.api.eventbus.event.container;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.blockentity.interfaces.BlockEntityFurnace;
import org.allaymc.api.eventbus.event.CancellableEvent;
import org.allaymc.api.item.ItemStack;

/**
 * Called when a furnace consumes fuel to continue smelting.
 *
 * @author daoge_cmd
 */
@Getter
@CallerThread(ThreadType.DIMENSION)
public class FurnaceConsumeFuelEvent extends ContainerEvent implements CancellableEvent {
    /**
     * The furnace consuming the fuel.
     */
    protected BlockEntityFurnace furnace;
    /**
     * The fuel item stack being consumed.
     */
    protected ItemStack fuel;
    /**
     * Yakit yandiktan sonra yakit yuvasina konacak esya; {@code null} ise motorun kendi kurali
     * (lav kovasi bos kovaya doner, digerleri bir adet eksilir). Birden fazla yakit tasiyan ozel
     * esyalar (GearsCore ametist lav kovasi) bir eksik dolu halini buraya yazar.
     */
    @Setter
    protected ItemStack residue;

    public FurnaceConsumeFuelEvent(BlockEntityFurnace furnace, ItemStack fuel) {
        this.furnace = furnace;
        this.fuel = fuel;
    }
}
