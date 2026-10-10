package org.allaymc.server.entity.component.mob;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.data.ItemTags;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.api.world.sound.SoundNames;
import org.joml.Vector3d;

import java.util.concurrent.ThreadLocalRandom;

public class EntityMooshroomBaseComponentImpl extends EntityMobBaseComponentImpl {

    public static final int RED = 0;
    public static final int BROWN = 1;

    public EntityMooshroomBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.9, 1.3);
        variant(() -> ThreadLocalRandom.current().nextInt(1000) == 0 ? BROWN : RED);
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player == null || itemStack == null || isBaby()) {
            return super.onInteract(player, itemStack);
        }

        var type = itemStack.getItemType();
        if (ItemTags.isShears(itemStack.getItemType())) {
            shearIntoCow(player, itemStack);
            return true;
        }
        if (type == ItemTypes.BOWL) {
            player.tryConsumeItemInHand();
            player.tryAddItem(ItemTypes.MUSHROOM_STEW.createItemStack(1));
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_MOOSHROOM_EAT));
            return true;
        }
        if (type == ItemTypes.BUCKET) {
            player.tryConsumeItemInHand();
            player.tryAddItem(ItemTypes.MILK_BUCKET.createItemStack(1));
            getDimension().addSound(location, SimpleSound.MILKING);
            return true;
        }

        return super.onInteract(player, itemStack);
    }

    protected void shearIntoCow(EntityPlayer player, ItemStack shears) {
        var pos = new Vector3d(location.x(), location.y() + 1, location.z());
        var mushroom = variant == BROWN ? ItemTypes.BROWN_MUSHROOM : ItemTypes.RED_MUSHROOM;
        getDimension().dropItem(mushroom.createItemStack(5), pos);
        shears.tryIncreaseDamage(1);
        var inventory = player.getContainer(ContainerTypes.INVENTORY);
        inventory.notifySlotChange(inventory.getHandSlot());
        getDimension().addSound(pos, new CustomSound(SoundNames.MOB_SHEEP_SHEAR));
        convertsTo(() -> EntityTypes.COW, 0, entity -> true, null);
        convert();
    }
}
