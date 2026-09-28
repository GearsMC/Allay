package org.allaymc.server.block.component;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.component.BlockBlockEntityHolderComponent;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.blockentity.interfaces.BlockEntityJukebox;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemMusicDiscStack;
import org.allaymc.api.math.position.Position3d;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.message.I18n;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.component.annotation.Dependency;

import java.util.List;

/**
 * @author IWareQ
 */
public class BlockJukeboxBaseComponentImpl extends BlockBaseComponentImpl {
    @Dependency
    BlockBlockEntityHolderComponent<BlockEntityJukebox> blockEntityHolderComponent;

    public BlockJukeboxBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean onInteract(ItemStack itemStack, Dimension dimension, PlayerInteractInfo interactInfo) {
        if (super.onInteract(itemStack, dimension, interactInfo)) {
            return true;
        }

        var blockEntity = blockEntityHolderComponent.getBlockEntity(new Position3i(interactInfo.clickedBlockPos(), dimension));
        var oldMusicDiscItem = blockEntity.getRecordItem();
        if (oldMusicDiscItem != null) {
            blockEntity.stop();
            silencePredictedRecord(interactInfo.player(), oldMusicDiscItem);
            blockEntity.setRecordItem(null);
            dimension.dropItem(oldMusicDiscItem, new Position3d(blockEntity.getPosition()).add(0.5, 1, 0.5));
            return true;
        } else if (itemStack instanceof ItemMusicDiscStack musicDiscItem) {
            var stored = musicDiscItem.copy();
            stored.setCount(1);
            blockEntity.setRecordItem(stored);
            interactInfo.player().tryConsumeItemInHand();
            blockEntity.play();
            var controller = interactInfo.player().getController();
            if (controller != null && controller.getLoginData() != null && controller.getLoginData().getLangCode() != null) {
                var lang = controller.getLoginData().getLangCode();
                var discName = I18n.get().tr(lang, musicDiscItem.getDiscType().getTranslationKey());
                controller.sendJukeboxPopup(I18n.get().tr(lang, TrKeys.MC_RECORD_NOWPLAYING, discName));
            }
            return true;
        }

        return false;
    }

    @Override
    public void onBreak(Block block, ItemStack usedItem, Entity entity, List<ItemStack> drops) {
        if (block.getDimension().getBlockEntity(block.getPosition()) instanceof BlockEntityJukebox jukebox) {
            jukebox.stop();
            if (entity instanceof EntityPlayer player) {
                silencePredictedRecord(player, jukebox.getRecordItem());
            }
        }
        super.onBreak(block, usedItem, entity, drops);
    }

    private void silencePredictedRecord(EntityPlayer player, ItemStack record) {
        if (player == null) {
            return;
        }
        var controller = player.getController();
        if (controller == null) {
            return;
        }
        controller.viewSound(SimpleSound.MUSIC_DISC_END, player.getLocation(), true);
        if (record instanceof ItemMusicDiscStack disc) {
            controller.stopSound(disc.getDiscType().soundName());
        }
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        var jukebox = blockEntityHolderComponent.getBlockEntity(block.getPosition());
        if (jukebox == null) {
            return 0;
        }
        var record = jukebox.getRecordItem();
        if (record == null) {
            return 0;
        }
        // Return signal based on disc type (1-15)
        // Each disc type has a unique comparator output; ozel plak 1 verir (Bedrock record bileseni).
        return record instanceof ItemMusicDiscStack disc ? disc.getComparatorSignal() : 1;
    }
}
