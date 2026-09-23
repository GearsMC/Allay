package org.allaymc.server.blockentity.component;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityJukeboxBaseComponent;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemMusicDiscStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.utils.NBTIO;
import org.allaymc.api.world.sound.MusicDiscPlaySound;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.block.component.event.CBlockOnReplaceEvent;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author IWareQ | daoge_cmd
 */
@Slf4j
public class BlockEntityJukeboxBaseComponentImpl extends BlockEntityBaseComponentImpl implements BlockEntityJukeboxBaseComponent {
    protected static final String TAG_RECORD_ITEM = "RecordItem";

    // Vanilla disk ya da eklentinin ozel plagi; ikisi de ayni yuvada durur.
    private ItemStack musicDiscItem;

    @Override
    public ItemMusicDiscStack getMusicDiscItem() {
        return this.musicDiscItem instanceof ItemMusicDiscStack disc ? disc : null;
    }

    @Override
    public void setMusicDiscItem(ItemMusicDiscStack musicDiscItem) {
        setRecordItem(musicDiscItem);
    }

    @Override
    public ItemStack getRecordItem() {
        return this.musicDiscItem;
    }

    @Override
    public void setRecordItem(ItemStack item) {
        this.musicDiscItem = item;
        // Update comparators that may be reading this jukebox
        this.getDimension().updateComparatorOutputLevel(this.getPosition());
    }

    public BlockEntityJukeboxBaseComponentImpl(BlockEntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public void play() {
        if (this.musicDiscItem instanceof ItemMusicDiscStack disc) {
            this.getDimension().addSound(this.getPosition(), new MusicDiscPlaySound(disc.getDiscType()));
        }
    }

    @Override
    public void stop() {
        this.getDimension().addSound(this.getPosition(), SimpleSound.MUSIC_DISC_END);
    }

    @EventHandler
    protected void onBlockReplace(CBlockOnReplaceEvent event) {
        if (this.musicDiscItem != null) {
            var current = event.getCurrentBlock();
            var pos = current.getPosition();
            var rand = ThreadLocalRandom.current();

            current.getDimension().dropItem(this.musicDiscItem, new Vector3d(
                    pos.x() + rand.nextDouble(0.5) + 0.25,
                    pos.y() + rand.nextDouble(0.5) + 0.25,
                    pos.z() + rand.nextDouble(0.5) + 0.25
            ));
            this.musicDiscItem = null;
            this.stop();
        }
    }

    @Override
    public NbtMap saveNBT() {
        var savedNbt = super.saveNBT();
        if (musicDiscItem != null) {
            savedNbt = savedNbt.toBuilder()
                    .putCompound(TAG_RECORD_ITEM, this.musicDiscItem.saveNBT())
                    .build();
        }
        return savedNbt;
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForCompound(TAG_RECORD_ITEM, value -> {
            var item = NBTIO.getAPI().fromItemStackNBT(value);
            // Ozel plak (eklenti esyasi) de korunur; yalnizca okunamayan kayit atlanir.
            if (item != null && item.getItemType() != ItemTypes.AIR) {
                this.musicDiscItem = item;
            } else {
                log.warn("Invalid music disc item {} in jukebox at {}", item.getItemType().getIdentifier(), this.getPosition());
            }
        });
    }
}
