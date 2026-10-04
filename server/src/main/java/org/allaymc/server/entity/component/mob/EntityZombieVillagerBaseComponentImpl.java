package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class EntityZombieVillagerBaseComponentImpl extends EntityMobBaseComponentImpl {

    protected static final String TAG_CURING = "Curing";
    protected static final String TAG_CURE_DELAY = "CureDelay";

    @Dependency
    protected EntityLivingComponent livingComponent;

    protected volatile boolean curing;

    public EntityZombieVillagerBaseComponentImpl(EntityInitInfo initInfo, Supplier<EntityType<?>> cureTarget) {
        super(initInfo, 0.6, 1.9);
        variant(() -> ThreadLocalRandom.current().nextInt(0, 15));
        markVariant(() -> ThreadLocalRandom.current().nextInt(0, 7));
        convertsTo(cureTarget, 3600, entity -> curing, SoundNames.MOB_ZOMBIE_UNFECT);
    }

    public boolean isCuring() {
        return curing;
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player != null && itemStack != null && !curing && itemStack.getItemType() == ItemTypes.GOLDEN_APPLE
                && livingComponent.hasEffect(EffectTypes.WEAKNESS)) {
            curing = true;
            conversionDelay = ThreadLocalRandom.current().nextInt(3600, 6001);
            player.tryConsumeItemInHand();
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_ZOMBIE_REMEDY));
            broadcastState();
            return true;
        }

        return super.onInteract(player, itemStack);
    }

    @EventHandler
    protected void onCuringLoadNBT(CEntityLoadNBTEvent event) {
        var nbt = event.getNbt();
        curing = nbt.getBoolean(TAG_CURING, false);
        conversionDelay = nbt.getInt(TAG_CURE_DELAY, conversionDelay);
    }

    @EventHandler
    protected void onCuringSaveNBT(CEntitySaveNBTEvent event) {
        event.getNbt().putBoolean(TAG_CURING, curing);
        event.getNbt().putInt(TAG_CURE_DELAY, conversionDelay);
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        super.writeMetadata(metadata);
        metadata.setFlag(EntityFlag.SHAKING, curing);
    }
}
