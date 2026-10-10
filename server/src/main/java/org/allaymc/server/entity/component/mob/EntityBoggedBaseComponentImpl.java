package org.allaymc.server.entity.component.mob;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.data.ItemTags;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.EntityMetadataContributor;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.allaymc.server.entity.component.humanlike.EntityArmedBaseComponentImpl;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

public class EntityBoggedBaseComponentImpl extends EntityArmedBaseComponentImpl implements EntityMetadataContributor {

    protected static final String TAG_SHEARED = "Sheared";

    protected volatile boolean sheared;

    public EntityBoggedBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, () -> ItemTypes.BOW, 0.6, 1.9);
    }

    public boolean isSheared() {
        return sheared;
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player == null || itemStack == null || sheared || !ItemTags.isShears(itemStack.getItemType())) {
            return false;
        }

        sheared = true;
        broadcastState();
        var pos = new Vector3d(location.x(), location.y() + 1, location.z());
        var rand = ThreadLocalRandom.current();
        getDimension().dropItem(ItemTypes.RED_MUSHROOM.createItemStack(1), pos);
        getDimension().dropItem((rand.nextBoolean() ? ItemTypes.RED_MUSHROOM : ItemTypes.BROWN_MUSHROOM).createItemStack(1), pos);
        itemStack.tryIncreaseDamage(1);
        var inventory = player.getContainer(ContainerTypes.INVENTORY);
        inventory.notifySlotChange(inventory.getHandSlot());
        getDimension().addSound(pos, new CustomSound(SoundNames.MOB_SHEEP_SHEAR));
        return true;
    }

    @EventHandler
    protected void onBoggedLoadNBT(CEntityLoadNBTEvent event) {
        sheared = event.getNbt().getBoolean(TAG_SHEARED, false);
    }

    @EventHandler
    protected void onBoggedSaveNBT(CEntitySaveNBTEvent event) {
        event.getNbt().putBoolean(TAG_SHEARED, sheared);
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        metadata.setFlag(EntityFlag.SHEARED, sheared);
    }
}
