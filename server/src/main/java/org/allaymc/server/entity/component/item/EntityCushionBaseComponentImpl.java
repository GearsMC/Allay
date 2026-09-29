package org.allaymc.server.entity.component.item;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityCushionBaseComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.interfaces.EntityCushion;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.utils.DyeColor;
import org.allaymc.api.world.particle.BlockBreakParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.EntityBaseComponentImpl;
import org.allaymc.server.entity.component.event.CEntityAfterDamageEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

public class EntityCushionBaseComponentImpl extends EntityBaseComponentImpl implements EntityCushionBaseComponent {

    private static final String TAG_COLOR = "Color";
    private static final int SUPPORT_CHECK_PERIOD = 100;

    @Dependency
    protected EntityLivingComponent livingComponent;

    private DyeColor color = DyeColor.WHITE;
    private boolean broken;

    public EntityCushionBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
        this.immobile = true;
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    @Override
    public void setColor(DyeColor color) {
        this.color = color;
        broadcastState();
    }

    @Override
    public AABBdc getBaseAABB() {
        if (customBaseAABB != null) {
            return customBaseAABB;
        }
        return new AABBd(-0.4995, 0, -0.4995, 0.4995, 0.249, 0.4995);
    }

    @Override
    public org.joml.Vector3dc getPassengerSeatOffset() {
        return new Vector3d(0, 1.25, 0);
    }

    @Override
    public boolean onInteract(EntityPlayer player, ItemStack itemStack) {
        if (player.getGameMode() == GameMode.SPECTATOR || getPassenger() != null) {
            return false;
        }
        var vehicle = player.getRidingVehicle();
        if (vehicle instanceof EntityCushion) {
            vehicle.dismountPassenger(false);
        }
        return mountPassenger(player);
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForByte(TAG_COLOR, value -> {
            int index = value & 0xff;
            if (index < DyeColor.values().length) {
                this.color = DyeColor.from(index);
            }
        });
    }

    @Override
    public NbtMap saveNBT() {
        return super.saveNBT().toBuilder()
                .putByte(TAG_COLOR, (byte) color.ordinal())
                .build();
    }

    @EventHandler
    protected void onTick(CEntityTickEvent event) {
        if (broken || getTick() % SUPPORT_CHECK_PERIOD != 0) {
            return;
        }
        if (!hasSupport()) {
            breakApart();
        }
    }

    @EventHandler
    protected void onDamage(CEntityAfterDamageEvent event) {
        breakApart();
    }

    private boolean hasSupport() {
        var location = getLocation();
        int x = (int) Math.floor(location.x());
        int y = (int) Math.floor(location.y()) - 1;
        int z = (int) Math.floor(location.z());
        var state = getDimension().getBlockState(x, y, z);
        return supportsCushion(state);
    }

    private void breakApart() {
        if (broken) {
            return;
        }
        broken = true;
        if (getPassenger() != null) {
            dismountPassenger(true);
        }

        var drop = true;
        var lastDamage = livingComponent.getLastDamage();
        if (lastDamage != null &&
            lastDamage.getAttacker() instanceof EntityPlayer player &&
            player.getGameMode() == GameMode.CREATIVE) {
            drop = false;
        }

        var dimension = getDimension();
        var location = getLocation();
        dimension.addSound(location, new CustomSound(SoundNames.ENTITY_CUSHION_BREAK));
        dimension.addParticle(location.add(0, 0.5, 0, new Vector3d()), new BlockBreakParticle(wool(color)));
        if (drop) {
            dimension.dropItem(itemOf(color).createItemStack(), location);
        }
        remove();
    }

    public static boolean supportsCushion(BlockState state) {
        var type = state.getBlockType();
        if (type.hasBlockTag(BlockTags.WATER) || type.hasBlockTag(BlockTags.LAVA)) {
            return false;
        }
        return state.getBlockStateData().hasCollision();
    }

    public static DyeColor colorOf(ItemType<?> itemType) {
        if (itemType == ItemTypes.ORANGE_CUSHION) return DyeColor.ORANGE;
        if (itemType == ItemTypes.MAGENTA_CUSHION) return DyeColor.MAGENTA;
        if (itemType == ItemTypes.LIGHT_BLUE_CUSHION) return DyeColor.LIGHT_BLUE;
        if (itemType == ItemTypes.YELLOW_CUSHION) return DyeColor.YELLOW;
        if (itemType == ItemTypes.LIME_CUSHION) return DyeColor.LIME;
        if (itemType == ItemTypes.PINK_CUSHION) return DyeColor.PINK;
        if (itemType == ItemTypes.GRAY_CUSHION) return DyeColor.GRAY;
        if (itemType == ItemTypes.LIGHT_GRAY_CUSHION) return DyeColor.LIGHT_GRAY;
        if (itemType == ItemTypes.CYAN_CUSHION) return DyeColor.CYAN;
        if (itemType == ItemTypes.PURPLE_CUSHION) return DyeColor.PURPLE;
        if (itemType == ItemTypes.BLUE_CUSHION) return DyeColor.BLUE;
        if (itemType == ItemTypes.BROWN_CUSHION) return DyeColor.BROWN;
        if (itemType == ItemTypes.GREEN_CUSHION) return DyeColor.GREEN;
        if (itemType == ItemTypes.RED_CUSHION) return DyeColor.RED;
        if (itemType == ItemTypes.BLACK_CUSHION) return DyeColor.BLACK;
        return DyeColor.WHITE;
    }

    public static ItemType<?> itemOf(DyeColor color) {
        return switch (color) {
            case ORANGE -> ItemTypes.ORANGE_CUSHION;
            case MAGENTA -> ItemTypes.MAGENTA_CUSHION;
            case LIGHT_BLUE -> ItemTypes.LIGHT_BLUE_CUSHION;
            case YELLOW -> ItemTypes.YELLOW_CUSHION;
            case LIME -> ItemTypes.LIME_CUSHION;
            case PINK -> ItemTypes.PINK_CUSHION;
            case GRAY -> ItemTypes.GRAY_CUSHION;
            case LIGHT_GRAY -> ItemTypes.LIGHT_GRAY_CUSHION;
            case CYAN -> ItemTypes.CYAN_CUSHION;
            case PURPLE -> ItemTypes.PURPLE_CUSHION;
            case BLUE -> ItemTypes.BLUE_CUSHION;
            case BROWN -> ItemTypes.BROWN_CUSHION;
            case GREEN -> ItemTypes.GREEN_CUSHION;
            case RED -> ItemTypes.RED_CUSHION;
            case BLACK -> ItemTypes.BLACK_CUSHION;
            case WHITE -> ItemTypes.WHITE_CUSHION;
        };
    }

    public static BlockState wool(DyeColor color) {
        var type = switch (color) {
            case ORANGE -> BlockTypes.ORANGE_WOOL;
            case MAGENTA -> BlockTypes.MAGENTA_WOOL;
            case LIGHT_BLUE -> BlockTypes.LIGHT_BLUE_WOOL;
            case YELLOW -> BlockTypes.YELLOW_WOOL;
            case LIME -> BlockTypes.LIME_WOOL;
            case PINK -> BlockTypes.PINK_WOOL;
            case GRAY -> BlockTypes.GRAY_WOOL;
            case LIGHT_GRAY -> BlockTypes.LIGHT_GRAY_WOOL;
            case CYAN -> BlockTypes.CYAN_WOOL;
            case PURPLE -> BlockTypes.PURPLE_WOOL;
            case BLUE -> BlockTypes.BLUE_WOOL;
            case BROWN -> BlockTypes.BROWN_WOOL;
            case GREEN -> BlockTypes.GREEN_WOOL;
            case RED -> BlockTypes.RED_WOOL;
            case BLACK -> BlockTypes.BLACK_WOOL;
            case WHITE -> BlockTypes.WHITE_WOOL;
        };
        return type.getDefaultState();
    }
}
