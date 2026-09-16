package org.allaymc.server.block.type;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.property.type.BlockPropertyType;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.item.ItemStack;
import org.cloudburstmc.nbt.NbtMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allay'in tanımadığı bir blok durumunun dünya verisindeki özgün NBT'sini taşır.
 *
 * <p>GearsMC fork: tanınmayan durum eskiden doğrudan {@link BlockTypes#UNKNOWN} oluyordu. Bölüm ya da saksı gibi
 * blok varlığı yeniden kaydedilince özgün veri kalıcı olarak siliniyordu ve bunu kimse fark etmiyordu. Tanınmayan
 * durum üç yerden gelir: daha yeni sürümle yazılmış veri (PocketMine/Altay 1.26.50 yazıyor), Allay'in eski bir
 * sürüme geri alınması ve artık kayıtlı olmayan bir eklenti bloğu.</p>
 *
 * <p>Motor ve istemci için bu durum {@code minecraft:unknown}'ın varsayılan durumuyla aynıdır: tür, hash, davranış
 * ve ağ kimliği ondan gelir. Tek fark {@link #getBlockStateNBT()}: kayıtta özgün NBT aynen geri yazılır, Allay durumu
 * ileride tanıdığında blok kendiliğinden geri gelir. Eşitlik özgün NBT'ye göre olduğu için farklı tanınmayan
 * durumlar aynı palette birbirine karışmaz.</p>
 *
 * @param originalTag dünya verisinden okunan, güncelleyiciden geçmemiş NBT (sürüm alanı dahil)
 */
@Slf4j
public record PreservedBlockState(NbtMap originalTag) implements BlockState {

    private static final Set<String> REPORTED_NAMES = ConcurrentHashMap.newKeySet();

    /**
     * Özgün NBT'yi saran durumu üretir. Aynı blok adı için oturumda bir kez uyarı yazar; bölüm başına yazmak bir dünya
     * açılışında binlerce satır üretirdi.
     */
    public static PreservedBlockState of(NbtMap originalTag) {
        if (REPORTED_NAMES.add(originalTag.getString("name"))) {
            log.warn("Unrecognized block state {} is loaded as unknown block, its original data will be kept on save", originalTag);
        }
        return new PreservedBlockState(originalTag);
    }

    private static BlockState unknown() {
        return BlockTypes.UNKNOWN.getDefaultState();
    }

    @Override
    public BlockType<?> getBlockType() {
        return unknown().getBlockType();
    }

    @Override
    public int blockStateHash() {
        return unknown().blockStateHash();
    }

    @Override
    public long specialValue() {
        return unknown().specialValue();
    }

    @Override
    public Map<BlockPropertyType<?>, BlockPropertyType.BlockPropertyValue<?, ?, ?>> getPropertyValues() {
        return unknown().getPropertyValues();
    }

    @Override
    public BlockState setPropertyValues(List<BlockPropertyType.BlockPropertyValue<?, ?, ?>> propertyValues) {
        return unknown().setPropertyValues(propertyValues);
    }

    @Override
    public <DATATYPE, PROPERTY extends BlockPropertyType<DATATYPE>> DATATYPE getPropertyValue(PROPERTY property) {
        return unknown().getPropertyValue(property);
    }

    @Override
    public BlockState setPropertyValue(BlockPropertyType.BlockPropertyValue<?, ?, ?> propertyValue) {
        return unknown().setPropertyValue(propertyValue);
    }

    @Override
    public <DATATYPE, PROPERTY extends BlockPropertyType<DATATYPE>> BlockState setPropertyValue(PROPERTY property, DATATYPE value) {
        return unknown().setPropertyValue(property, value);
    }

    @Override
    public NbtMap getBlockStateNBT() {
        return originalTag;
    }

    @Override
    public ItemStack toItemStack() {
        return unknown().toItemStack();
    }
}
