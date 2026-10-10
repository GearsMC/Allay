package org.allaymc.server.entity.component.mob;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityContainerHolderComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.interfaces.EntityAnimal;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.data.ItemTags;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.world.WorldViewer;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.server.entity.component.EntityAngerableBaseComponentImpl;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.component.EntityMetadataContributor;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.allaymc.server.entity.impl.EntityImpl;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EntityMobBaseComponentImpl extends EntityBaseComponentImpl implements EntityMetadataContributor {

    protected static final String TAG_VARIANT = "Variant";
    protected static final String TAG_MARK_VARIANT = "MarkVariant";
    protected static final String TAG_SHEARED = "Sheared";
    protected static final String TAG_CONVERSION_TICKS = "ConversionTicks";
    protected static final String TAG_DROP_COOLDOWN = "DropCooldown";

    protected final AABBdc baseAABB;

    protected float babyScale = 0.5f;
    protected IntSupplier variantRoller;
    protected IntSupplier markVariantRoller;
    protected Supplier<List<ItemStack>> shearDrops;
    protected String shearSound;
    protected EntityFlag huntingFlag;
    protected Supplier<EntityType<?>> conversionTarget;
    protected Predicate<Entity> conversionCondition;
    protected int conversionDelay;
    protected String conversionSound;
    protected IntSupplier dropInterval;
    protected Supplier<ItemStack> periodicDrop;
    protected String periodicDropSound;
    protected Supplier<ItemType<?>> weaponSupplier;
    protected Supplier<ItemType<?>> offhandSupplier;

    protected volatile int variant;
    protected volatile int markVariant;
    protected volatile boolean sheared;
    protected volatile boolean hunting;
    protected boolean broadcastHunting;
    protected int conversionTicks;
    protected int dropCooldown = -1;

    public EntityMobBaseComponentImpl(EntityInitInfo initInfo, double width, double height) {
        super(initInfo);
        var halfWidth = width / 2;
        this.baseAABB = new AABBd(-halfWidth, 0.0, -halfWidth, halfWidth, height, halfWidth);
    }

    public EntityMobBaseComponentImpl babyScale(float babyScale) {
        this.babyScale = babyScale;
        return this;
    }

    public EntityMobBaseComponentImpl variant(IntSupplier roller) {
        this.variantRoller = roller;
        return this;
    }

    public EntityMobBaseComponentImpl markVariant(IntSupplier roller) {
        this.markVariantRoller = roller;
        return this;
    }

    public EntityMobBaseComponentImpl shearable(String sound, Supplier<List<ItemStack>> drops) {
        this.shearSound = sound;
        this.shearDrops = drops;
        return this;
    }

    public EntityMobBaseComponentImpl huntingFlag(EntityFlag flag) {
        this.huntingFlag = flag;
        return this;
    }

    public EntityMobBaseComponentImpl convertsTo(Supplier<EntityType<?>> target, int delayTicks,
                                                 Predicate<Entity> condition, String sound) {
        this.conversionTarget = target;
        this.conversionDelay = delayTicks;
        this.conversionCondition = condition;
        this.conversionSound = sound;
        return this;
    }

    public EntityMobBaseComponentImpl periodicDrop(IntSupplier interval, Supplier<ItemStack> drop, String sound) {
        this.dropInterval = interval;
        this.periodicDrop = drop;
        this.periodicDropSound = sound;
        return this;
    }

    public EntityMobBaseComponentImpl weapon(Supplier<ItemType<?>> weapon) {
        this.weaponSupplier = weapon;
        return this;
    }

    public EntityMobBaseComponentImpl offhand(Supplier<ItemType<?>> offhand) {
        this.offhandSupplier = offhand;
        return this;
    }

    @Override
    public AABBdc getBaseAABB() {
        return baseAABB;
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
        broadcastState();
    }

    public int getMarkVariant() {
        return markVariant;
    }

    public void setMarkVariant(int markVariant) {
        this.markVariant = markVariant;
        broadcastState();
    }

    public boolean isSheared() {
        return sheared;
    }

    public void setSheared(boolean sheared) {
        this.sheared = sheared;
        broadcastState();
    }

    public int getConversionTicks() {
        return conversionTicks;
    }

    @Override
    public void spawnTo(WorldViewer viewer) {
        var alreadyViewing = getViewers().contains(viewer);
        super.spawnTo(viewer);
        if (alreadyViewing || !(thisEntity instanceof EntityContainerHolderComponent holder)
                || !holder.hasContainer(ContainerTypes.ENTITY_HAND)) {
            return;
        }

        var equipped = (Entity & EntityContainerHolderComponent) thisEntity;
        viewer.viewEntityHand(equipped);
        if (holder.hasContainer(ContainerTypes.OFFHAND)) {
            viewer.viewEntityOffhand(equipped);
        }
        if (holder.hasContainer(ContainerTypes.ARMOR)) {
            viewer.viewEntityArmors(equipped);
        }
    }

    public boolean isBaby() {
        return thisEntity instanceof EntityBabyComponent baby && baby.isBaby();
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player == null || itemStack == null) {
            return false;
        }

        if (shearDrops != null && !sheared && !isBaby() && ItemTags.isShears(itemStack.getItemType())) {
            shear(player, itemStack);
            return true;
        }

        return tryFeed(player, itemStack);
    }

    protected void shear(EntityPlayer player, ItemStack shears) {
        setSheared(true);
        var dropPos = new Vector3d(location.x(), location.y() + getEyeHeight() * 0.5, location.z());
        for (var drop : shearDrops.get()) {
            getDimension().dropItem(drop, dropPos);
        }
        shears.tryIncreaseDamage(1);
        var inventory = player.getContainer(ContainerTypes.INVENTORY);
        inventory.notifySlotChange(inventory.getHandSlot());
        if (shearSound != null) {
            getDimension().addSound(dropPos, new CustomSound(shearSound));
        }
    }

    protected boolean tryFeed(EntityPlayer player, ItemStack itemStack) {
        if (!(thisEntity instanceof EntityAnimal animal) || !animal.isBreedingItem(itemStack)) {
            return false;
        }

        animal.getMemoryStorage().put(MemoryTypes.LAST_BE_FEED_TIME, thisEntity.getTick());
        animal.getMemoryStorage().put(MemoryTypes.LAST_FEED_PLAYER, player.getRuntimeId());
        player.tryConsumeItemInHand();
        return true;
    }

    @EventHandler
    protected void onMobTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive() || thisEntity.willBeDespawnedLater()) {
            return;
        }

        tickHunting();
        tickConversion();
        tickPeriodicDrop();
    }

    protected void tickHunting() {
        if (huntingFlag == null || !(thisEntity instanceof EntityIntelligent intelligent)) {
            return;
        }

        hunting = EntityAngerableBaseComponentImpl.isHunting(intelligent);
        if (hunting != broadcastHunting) {
            broadcastHunting = hunting;
            broadcastState();
        }
    }

    protected void tickConversion() {
        if (conversionTarget == null) {
            return;
        }

        if (!conversionCondition.test(thisEntity)) {
            conversionTicks = 0;
            return;
        }

        if (++conversionTicks >= conversionDelay) {
            convert();
        }
    }

    protected void convert() {
        var type = conversionTarget.get();
        if (type == null) {
            return;
        }

        var loc = location;
        var converted = type.createEntity(EntityInitInfo.builder()
                .dimension(getDimension())
                .pos(loc.x(), loc.y(), loc.z())
                .rot(loc.yaw(), loc.pitch())
                .build());
        if (converted instanceof EntityBabyComponent baby && isBaby()) {
            baby.setBaby(true);
        }
        if (getNameTag() != null) {
            converted.setNameTag(getNameTag());
        }
        if (converted instanceof EntityLivingComponent living && thisEntity instanceof EntityLivingComponent old) {
            living.setHealth(Math.min(living.getMaxHealth(), Math.max(1, old.getHealth())));
        }
        copyVariant(converted);
        getDimension().getEntityManager().addEntity(converted);
        if (conversionSound != null) {
            getDimension().addSound(new Vector3d(loc.x(), loc.y(), loc.z()), new CustomSound(conversionSound));
        }
        conversionTarget = null;
        thisEntity.remove();
    }

    /**
     * Copies biome and profession onto a converted mob that stores the same values.
     * Targets without a roller keep their own freshly rolled identity.
     */
    protected void copyVariant(Entity converted) {
        if (!(converted instanceof EntityImpl impl) || !(impl.getBaseComponent() instanceof EntityMobBaseComponentImpl target)) {
            return;
        }
        if (variantRoller != null && target.variantRoller != null) {
            target.setVariant(variant);
        }
        if (markVariantRoller != null && target.markVariantRoller != null) {
            target.setMarkVariant(markVariant);
        }
    }

    protected void tickPeriodicDrop() {
        if (periodicDrop == null || isBaby()) {
            return;
        }

        if (dropCooldown < 0) {
            dropCooldown = dropInterval.getAsInt();
        }
        if (--dropCooldown > 0) {
            return;
        }

        dropCooldown = dropInterval.getAsInt();
        var pos = new Vector3d(location.x(), location.y() + 0.5, location.z());
        getDimension().dropItem(periodicDrop.get(), pos);
        if (periodicDropSound != null) {
            getDimension().addSound(pos, new CustomSound(periodicDropSound));
        }
    }

    @EventHandler
    protected void onMobLoadNBT(CEntityLoadNBTEvent event) {
        var nbt = event.getNbt();
        if (variantRoller != null) {
            variant = nbt.containsKey(TAG_VARIANT) ? nbt.getInt(TAG_VARIANT) : variantRoller.getAsInt();
        }
        if (markVariantRoller != null) {
            markVariant = nbt.containsKey(TAG_MARK_VARIANT) ? nbt.getInt(TAG_MARK_VARIANT) : markVariantRoller.getAsInt();
        }
        if (shearDrops != null) {
            sheared = nbt.getBoolean(TAG_SHEARED, false);
        }
        equip();
        conversionTicks = nbt.getInt(TAG_CONVERSION_TICKS, 0);
        dropCooldown = nbt.getInt(TAG_DROP_COOLDOWN, -1);
    }

    protected void equip() {
        if (!(thisEntity instanceof EntityContainerHolderComponent holder) || !holder.hasContainer(ContainerTypes.ENTITY_HAND)) {
            return;
        }

        var hand = holder.getContainer(ContainerTypes.ENTITY_HAND);
        if (weaponSupplier != null && hand.getItemInHand().getItemType() == ItemTypes.AIR) {
            var weapon = weaponSupplier.get();
            if (weapon != null) {
                hand.setItemInHand(weapon.createItemStack());
            }
        }
        if (offhandSupplier != null && holder.hasContainer(ContainerTypes.OFFHAND)) {
            var offhandContainer = holder.getContainer(ContainerTypes.OFFHAND);
            if (offhandContainer.getItemStack(0).getItemType() == ItemTypes.AIR) {
                var item = offhandSupplier.get();
                if (item != null) {
                    offhandContainer.setItemStack(0, item.createItemStack());
                }
            }
        }
    }

    @EventHandler
    protected void onMobSaveNBT(CEntitySaveNBTEvent event) {
        var nbt = event.getNbt();
        if (variantRoller != null) {
            nbt.putInt(TAG_VARIANT, variant);
        }
        if (markVariantRoller != null) {
            nbt.putInt(TAG_MARK_VARIANT, markVariant);
        }
        if (shearDrops != null) {
            nbt.putBoolean(TAG_SHEARED, sheared);
        }
        if (conversionTarget != null) {
            nbt.putInt(TAG_CONVERSION_TICKS, conversionTicks);
        }
        if (periodicDrop != null) {
            nbt.putInt(TAG_DROP_COOLDOWN, dropCooldown);
        }
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        if (variantRoller != null) {
            metadata.put(EntityDataTypes.VARIANT, variant);
        }
        if (markVariantRoller != null) {
            metadata.put(EntityDataTypes.MARK_VARIANT, markVariant);
        }
        if (shearDrops != null) {
            metadata.setFlag(EntityFlag.SHEARED, sheared);
        }
        if (isBaby()) {
            metadata.put(EntityDataTypes.SCALE, babyScale * (float) getScale());
        }
        if (huntingFlag != null) {
            metadata.setFlag(huntingFlag, hunting);
        }
    }
}
