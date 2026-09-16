package org.allaymc.server.utils;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.property.type.BlockPropertyType;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.BlockEntity;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.utils.NBTIO;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.api.utils.identifier.InvalidIdentifierException;
import org.allaymc.server.block.type.PreservedBlockState;
import org.allaymc.server.entity.data.LegacyEntityNames;
import org.allaymc.api.world.Dimension;
import org.allaymc.server.network.ProtocolInfo;
import org.allaymc.updater.block.BlockStateUpdaters;
import org.allaymc.updater.item.ItemStateUpdaters;
import org.cloudburstmc.nbt.NbtMap;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author daoge_cmd
 */
@Slf4j
public class AllayNBTIO implements NBTIO {
    @Override
    public BlockState fromBlockStateNBT(NbtMap nbt) {
        var blockState = findBlockState(nbt);
        return blockState != null ? blockState : BlockTypes.UNKNOWN.getDefaultState();
    }

    /**
     * Dünya verisinden okunan blok durumu için {@link #fromBlockStateNBT} karşılığı.
     *
     * <p>GearsMC fork: tanınmayan durum bilinmeyen bloğa çevrilmez, özgün NBT'si {@link PreservedBlockState} içinde
     * kalır ve kayıtta aynen geri yazılır. API yöntemi sözleşmesi gereği {@code UNKNOWN} döndürmeye devam eder;
     * komut ya da eklentinin elle kurduğu durumun korunacak bir özgün verisi yoktur.</p>
     */
    public static BlockState fromSavedBlockStateNBT(NbtMap nbt) {
        var blockState = findBlockState(nbt);
        return blockState != null ? blockState : PreservedBlockState.of(nbt);
    }

    private static BlockState findBlockState(NbtMap nbt) {
        // GearsMC fork: sürümü daha yeni olan veri artık reddedilmiyor. PocketMine/Altay durumları kendi sürüm
        // numarasıyla (1.26.50) yazıyor ve bunların neredeyse hepsi Allay'in bildiği durumlar; chunk yolu
        // (ChunkSectionCodec) da önce tanımaya çalışıyor. Tanınmayan özellik ya da değer aşağıda null'a düşer.
        // Always update the nbt if we can't find the version field
        var version = nbt.getInt("version", 0);
        if (version < ProtocolInfo.BLOCK_STATE_VERSION_NUM) {
            nbt = BlockStateUpdaters.updateBlockState(nbt, ProtocolInfo.BLOCK_STATE_UPDATER.getVersion());
        }

        // Get the block type
        BlockType<?> blockType;
        try {
            blockType = Registries.BLOCKS.get(new Identifier(nbt.getString("name")));
        } catch (InvalidIdentifierException e) {
            blockType = null;
        }
        if (blockType == null) {
            log.warn("Unknown block type {}", nbt.getString("name"));
            return null;
        }

        // Add missing properties
        var states = nbt.getCompound("states");
        var builder = states.toBuilder();
        for (var entry : blockType.getDefaultState().getPropertyValues().entrySet()) {
            if (builder.containsKey(entry.getKey().getName())) {
                continue;
            }

            builder.put(entry.getKey().getName(), entry.getValue().getSerializedValue());
        }
        states = builder.build();

        // Create the block property value list
        var blockPropertyValues = new ArrayList<BlockPropertyType.BlockPropertyValue<?, ?, ?>>();
        for (var entry : states.entrySet()) {
            var propertyType = blockType.getProperties().get(entry.getKey());
            BlockPropertyType.BlockPropertyValue<?, ?, ?> value = null;
            if (propertyType != null) {
                try {
                    value = propertyType.tryCreateValue(entry.getValue());
                } catch (RuntimeException ignored) {
                    // Geçersiz değer: tanınmayan durum olarak aşağıda raporlanır
                }
            }
            if (value == null) {
                log.warn("Invalid block state {}", nbt);
                return null;
            }
            blockPropertyValues.add(value);
        }

        // Get the block state
        var blockState = blockType.ofState(blockPropertyValues);
        if (blockState == null) {
            log.warn("Invalid block state {}", nbt);
            return null;
        }

        return blockState;
    }

    @Override
    public ItemStack fromItemStackNBT(NbtMap nbt) {
        try {
            nbt = ItemStateUpdaters.updateItemState(nbt, ProtocolInfo.ITEM_STATE_UPDATER.getVersion());
            int count = nbt.getByte("Count", (byte) 1);
            int meta = nbt.getShort("Damage");
            var name = nbt.getString("Name");
            var itemType = Objects.requireNonNull(Registries.ITEMS.get(new Identifier(name)), "Unknown item type " + name + " while loading container items!");
            return itemType.createItemStack(
                    ItemStackInitInfo
                            .builder()
                            .count(count)
                            .meta(meta)
                            .extraTag(nbt.getCompound("tag"))
                            .build()
            );
        } catch (Throwable t) {
            log.error("Failed to load item from NBT", t);
            return ItemAirStack.AIR_STACK;
        }
    }

    @Override
    public Entity fromEntityNBT(Dimension dimension, NbtMap nbt) {
        var rawIdentifier = nbt.getString("identifier");
        // PocketMine vanilla varliklari eski kayit adiyla yazar ("Painting",
        // "Item", "Arrow"); eslemesiz okuma bunlari bilinmeyen sayip atlar ve
        // chunk yeniden yazilirken varlik kalici olarak kaybolur.
        var legacyIdentifier = LegacyEntityNames.resolve(rawIdentifier);
        if (legacyIdentifier != null) {
            rawIdentifier = legacyIdentifier;
        }
        Identifier identifier;
        try {
            identifier = new Identifier(rawIdentifier);
        } catch (InvalidIdentifierException e) {
            // PocketMine eklentileri varlik kimligine PHP sinif adi yazabiliyor
            // (ornegin brokiem\snpc\entity\CustomHuman). Kurucunun attigi istisna
            // cagirana kadar cikip o chunk'taki butun varliklarin okunmasini iptal
            // ediyordu; bicimsiz kimlik de bilinmeyen tip gibi atlanmali.
            log.warn("Malformed entity identifier {}", rawIdentifier);
            return null;
        }

        var entityType = Registries.ENTITIES.get(identifier);
        if (entityType == null) {
            log.warn("Unknown entity type {}", identifier);
            return null;
        }

        return entityType.createEntity(EntityInitInfo.builder().dimension(dimension).nbt(nbt).build());
    }

    @Override
    public BlockEntity fromBlockEntityNBT(Dimension dimension, NbtMap nbt) {
        var id = nbt.getString("id");
        var blockEntityType = Registries.BLOCK_ENTITIES.get(id);
        if (blockEntityType == null) {
            log.warn("Unknown block entity type: {}", id);
            return null;
        }

        return blockEntityType.createBlockEntity(BlockEntityInitInfo.builder().dimension(dimension).nbt(nbt).build());
    }
}
