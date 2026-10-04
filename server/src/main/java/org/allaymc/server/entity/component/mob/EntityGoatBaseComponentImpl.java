package org.allaymc.server.entity.component.mob;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.api.world.sound.SoundNames;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.concurrent.ThreadLocalRandom;

public class EntityGoatBaseComponentImpl extends EntityMobBaseComponentImpl {

    @Getter
    @Setter
    protected volatile long nextRamTick = -1;
    protected volatile boolean ramming;

    public EntityGoatBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.9, 1.3);
        variant(() -> ThreadLocalRandom.current().nextInt(50) == 0 ? 1 : 0);
    }

    public boolean isScreamer() {
        return variant == 1;
    }

    public void setRamming(boolean ramming) {
        if (this.ramming != ramming) {
            this.ramming = ramming;
            broadcastState();
        }
    }

    public int rollRamCooldown() {
        var rand = ThreadLocalRandom.current();
        return 20 * (isScreamer() ? rand.nextInt(5, 16) : rand.nextInt(30, 301));
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player != null && itemStack != null && itemStack.getItemType() == ItemTypes.BUCKET && !isBaby()) {
            player.tryConsumeItemInHand();
            player.tryAddItem(ItemTypes.MILK_BUCKET.createItemStack(1));
            if (isScreamer()) {
                getDimension().addSound(location, new CustomSound(SoundNames.MOB_GOAT_MILK_SCREAMER));
            } else {
                getDimension().addSound(location, SimpleSound.MILKING);
            }
            return true;
        }

        return super.onInteract(player, itemStack);
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        super.writeMetadata(metadata);
        metadata.setFlag(EntityFlag.RAM_ATTACK, ramming);
    }
}
