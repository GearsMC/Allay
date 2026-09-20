package org.allaymc.data.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import org.allaymc.api.utils.hash.HashUtils;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Blok verisini üretir: palet, durum başına fizik verisi, tür başına varsayılan durum ve etiketler.
 */
@UtilityClass
final class BlockDataImport {

    private static final String UNKNOWN = "minecraft:unknown";

    /**
     * Vanilla kahinin palet dökümünü ({@code --mode palette}) Allay'in {@code unpacked/block_palette.nbt} biçimine çevirir.
     *
     * <p>Palet ağda gelmiyor, istemci kendi içinde taşıyor; bu yüzden eskiden CloudburstMC/Data'dan alınıyordu. Kahin
     * onu oyunun kendisinden üretiyor (özellik adları Mojang meta verisinden, değer aralıkları BDS'te ölçülerek) ve
     * 26.50'de CloudburstMC paletiyle birebir aynı 22091 durumu veriyor.</p>
     *
     * <p><b>Sıra:</b> tür sırası vanilla ile aynı olsun diye ada göre FNV-1 (64 bit) ile sıralanır — CloudburstMC
     * paletindeki {@code name_hash} alanı tam olarak budur ve palet ona göre artan sırada. Tür içindeki durum sırası
     * vanilla'nın kendi iç sırası; tek bir odometre kuralıyla açıklanmıyor (1477 türün 172'si hiçbir düzeni izlemiyor),
     * bu yüzden kahinin ürettiği sıra korunur. Sıra yalnızca kendi yüz ölçümümüzü etkiler: {@code bds_connection_faces.json}
     * satırları palet sırasına göre numaralı, dolayısıyla palet değişince tablo yeniden ölçülmeli
     * ({@code run_oracle.py --mode faces}). Sunucu ve testler durumları hash'le okuduğu için sıradan etkilenmez.</p>
     *
     * @param stateVersion blok durumu sürümü; paletteki bütün girdilerde aynı ({@link BedrockDataImporter})
     */
    static NbtMap palette(JsonObject oraclePalette, int stateVersion) {
        record Entry(long nameHash, int index, NbtMap state) {
        }

        var states = oraclePalette.getAsJsonArray("palette");
        var entries = new ArrayList<Entry>(states.size());
        for (var index = 0; index < states.size(); index++) {
            var entry = states.get(index).getAsJsonObject();
            var name = entry.get("name").getAsString();
            entries.add(new Entry(fnv1(name), index, NbtMap.builder()
                    .putString("name", name)
                    .putCompound("states", stateCompound(entry.getAsJsonObject("states")))
                    .putInt("version", stateVersion)
                    .build()));
        }
        // Hash işaretsiz karşılaştırılır: yüksek bitli adlar yoksa sıra vanilla ile tutmaz.
        entries.sort(Comparator.comparing(Entry::nameHash, Long::compareUnsigned).thenComparingInt(Entry::index));

        var blocks = new ArrayList<NbtMap>(entries.size());
        entries.forEach(entry -> blocks.add(entry.state()));
        return NbtMap.builder().putList("blocks", NbtType.COMPOUND, blocks).build();
    }

    /** JSON durumları NBT'ye: mantıksal değer bayt, tam sayı int, geri kalanı dize. */
    private static NbtMap stateCompound(JsonObject states) {
        var builder = NbtMap.builder();
        for (var state : states.entrySet()) {
            var value = state.getValue().getAsJsonPrimitive();
            if (value.isBoolean()) {
                builder.putByte(state.getKey(), (byte) (value.getAsBoolean() ? 1 : 0));
            } else if (value.isNumber()) {
                builder.putInt(state.getKey(), value.getAsInt());
            } else {
                builder.putString(state.getKey(), value.getAsString());
            }
        }
        return builder.build();
    }

    /** Vanilla'nın blok adı hash'i: FNV-1 (FNV-1a değil), 64 bit. */
    private static long fnv1(String name) {
        var hash = 0xcbf29ce484222325L;
        for (var b : name.getBytes(StandardCharsets.UTF_8)) {
            hash *= 0x100000001b3L;
            hash ^= b & 0xff;
        }
        return hash;
    }

    /**
     * Tür başına varsayılan durum hash'i ve etiketler ({@code block_types.json}).
     *
     * <p>Varsayılan durum BDS dökümünden gelir ({@code BlockPermutation.resolve}). Script API durum listesine palette
     * olmayan eski adlar da ekleyebildiği için (çitte {@code wood_type}) palet girdisi "palet anahtarlarının hepsi
     * dökümdekiyle aynı" diye seçilir. Etiketlere önek eksikse {@code minecraft:} eklenir: Script API bazılarını
     * öneksiz döndürüyor, Endstone dökümü hep önekliydi.</p>
     *
     * @param currentBlockTypes {@code minecraft:unknown} için bugünkü değer korunur (ağda -2)
     */
    static JsonObject blockTypes(NbtMap palette, JsonObject dumpBlocks, JsonObject currentBlockTypes) {
        var statesByName = new TreeMap<String, List<NbtMap>>();
        for (var state : palette.getList("blocks", NbtType.COMPOUND)) {
            statesByName.computeIfAbsent(state.getString("name"), k -> new ArrayList<>()).add(state.getCompound("states"));
        }

        var result = new JsonObject();
        for (var entry : statesByName.entrySet()) {
            var name = entry.getKey();
            if (name.equals(UNKNOWN)) {
                result.add(name, currentBlockTypes.get(name).deepCopy());
                continue;
            }
            var vanilla = dumpBlocks.getAsJsonObject(name);
            if (vanilla == null || vanilla.has("error")) {
                throw new IllegalStateException("BDS dökümünde blok yok: " + name);
            }

            var vanillaStates = vanilla.getAsJsonObject("states");
            var matches = entry.getValue().stream().filter(states -> matches(states, vanillaStates)).toList();
            if (matches.size() != 1) {
                throw new IllegalStateException(name + " varsayılan durumu " + matches.size() + " palet girdisine uyuyor");
            }

            var type = new JsonObject();
            type.addProperty("defaultBlockStateHash", Integer.toUnsignedLong(stateHash(name, matches.getFirst())));
            var tags = new TreeSet<String>();
            vanilla.getAsJsonArray("tags").forEach(tag -> tags.add(namespaced(tag.getAsString())));
            if (!tags.isEmpty()) {
                var array = new JsonArray();
                tags.forEach(array::add);
                type.add("tags", array);
            }
            result.add(name, type);
        }
        return result;
    }

    /**
     * Etiket dosyası ({@code block_tags.json}, {@code item_tags.json}): tür ya da eşya başına etiketlerin toplamı.
     *
     * <p>Endstone'un iki dosyası da tam olarak bu toplamdı (bugünkü veride 38/38 ve 76/76 etiket birebir). Altay'ın etiket
     * dosyaları kullanılmaz: BDS'te elde tutulamayan eşyalarda (ör. {@code written_book}, eğitim yumurtaları) Endstone'dan
     * farklı ve bu farkı doğrulayacak başka kaynak yok; toplam alındığında etiketler tek doğruluk kaynağından gelir.</p>
     */
    static JsonObject aggregateTags(JsonObject entries) {
        var members = new TreeMap<String, TreeSet<String>>();
        for (var entry : entries.entrySet()) {
            var tags = entry.getValue().getAsJsonObject().getAsJsonArray("tags");
            if (tags == null) {
                continue;
            }
            tags.forEach(tag -> members.computeIfAbsent(tag.getAsString(), k -> new TreeSet<>()).add(entry.getKey()));
        }
        var result = new JsonObject();
        members.forEach((tag, names) -> {
            var array = new JsonArray();
            names.forEach(array::add);
            result.add(tag, array);
        });
        return result;
    }

    static int stateHash(String name, NbtMap states) {
        return HashUtils.fnv1a_32_nbt(NbtMap.builder()
                .putString("name", name)
                .putCompound("states", NbtMap.fromMap(new TreeMap<>(states)))
                .build());
    }

    static String namespaced(String identifier) {
        return identifier.contains(":") ? identifier : "minecraft:" + identifier;
    }

    private static boolean matches(NbtMap paletteStates, JsonObject vanillaStates) {
        for (var key : paletteStates.keySet()) {
            var vanilla = vanillaStates.get(key);
            if (vanilla == null || !String.valueOf(paletteStates.get(key)).equals(asPaletteValue(vanilla))) {
                return false;
            }
        }
        return true;
    }

    /** Script API boolean durumları {@code true/false}, palet bayt olarak {@code 1/0} tutar. */
    private static String asPaletteValue(JsonElement value) {
        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? "1" : "0";
        }
        return primitive.isNumber() ? String.valueOf(primitive.getAsInt()) : primitive.getAsString();
    }
}
