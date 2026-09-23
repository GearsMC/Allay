package org.allaymc.api.blockentity.component;

import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemMusicDiscStack;

/**
 * @author IWareQ
 */
public interface BlockEntityJukeboxBaseComponent extends BlockEntityBaseComponent {

    /**
     * Play the music disc inside the jukebox.
     */
    void play();

    /**
     * Stop the music disc inside the jukebox.
     */
    void stop();

    /**
     * Get the music disc inside the jukebox.
     *
     * @return the music disc item
     */
    ItemMusicDiscStack getMusicDiscItem();

    /**
     * Set the music disc inside the jukebox.
     *
     * @param item the music disc item
     */
    void setMusicDiscItem(ItemMusicDiscStack item);

    /**
     * Muzik kutusundaki plak; vanilla disk ya da eklentinin kaydettigi ozel plak olabilir.
     *
     * <p>Ozel plagi motor calmaz ({@link #play()} yalnizca vanilla diskte ses cikarir); sesi eklenti
     * yonetir. Kayit, kirilinca dusurme ve comparator (ozel plakta 1) ikisi icin de aynidir.</p>
     *
     * @return plak; bossa {@code null}
     */
    ItemStack getRecordItem();

    /**
     * Muzik kutusuna plak koyar ya da ({@code null}) bosaltir.
     *
     * @param item plak; vanilla disk ya da ozel plak
     */
    void setRecordItem(ItemStack item);
}
