package org.allaymc.server.blockentity.component;

import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Crafter'in blok varligi.
 *
 * <p>Esyalar konteyner bileseninde tutulur; burada yalnizca hangi slotlarin
 * devre disi birakildigini soyleyen bit maskesi saklanir. Maske
 * kaydedilmezse yoneticinin kapattigi slotlar chunk her yeniden yuklendiginde
 * geri acilir.</p>
 *
 * @see <a href="https://minecraft.wiki/w/Bedrock_Edition_level_format/Block_entity_format#Crafter">Crafter</a>
 */
public class BlockEntityCrafterBaseComponentImpl extends BlockEntityBaseComponentImpl {

    protected static final String TAG_DISABLED_SLOTS = "disabled_slots";

    /** Devre disi slotlarin bit maskesi. */
    protected int disabledSlots = 0;

    public BlockEntityCrafterBaseComponentImpl(BlockEntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public NbtMap saveNBT() {
        return super.saveNBT().toBuilder()
                .putInt(TAG_DISABLED_SLOTS, disabledSlots)
                .build();
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForInt(TAG_DISABLED_SLOTS, value -> this.disabledSlots = value);
    }
}
