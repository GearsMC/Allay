package org.allaymc.data.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.io.IOException;
import java.util.*;

/**
 * Eşya verisini üretir: {@code items_raw.json}, {@code item_components.nbt}, yaratıcı mod listesi.
 */
@UtilityClass
final class ItemDataImport {

    /** Endstone'un eşya başına alanları; kimlik ve etiketler ayrıca işlenir. */
    static final List<String> VALUE_FIELDS = List.of(
            "armorValue", "attackDamage", "enchantValue", "furnaceBurnDuration", "furnaceXPMultiplier",
            "isDamageable", "maxDamage", "maxStackSize", "toughnessValue"
    );

    /** Hiçbir kaynakta değeri olmayan ve benzeri de bulunmayan eşyalar varsayılan değer alır. */
    static final Set<String> DEFAULTED_ITEMS = Set.of("minecraft:photo_item", "minecraft:portfolio");

    private static final int FUEL_STEP_TICKS = 50;

    /**
     * {@code items_raw.json} üretir.
     *
     * <ul>
     *     <li>Kimlikler 26.50 {@code runtime_item_states.json}'dan gelir (1933 eşyanın 587'si kaydı).</li>
     *     <li>Bugün var olan eşyanın değerleri korunur; BDS'te görünüyorsa etiketleri 26.50 dökümünden alınır.</li>
     *     <li>Yeni ve BDS'te görünen eşyada yığın, dayanıklılık, etiket ve fırında ölçülen yakıt süresi dökümden gelir.
     *     Zırh, saldırı, büyü ve deneyim değerleri ölçülemiyor; yeni eşyaların hiçbiri alet/zırh/fırın çıktısı olmadığı
     *     için 0'dır, büyü yuvası ya da yakıt ölçüm sınırı görülürse araç durur.</li>
     *     <li>BDS'te görünmeyen yeni eşya {@link #derive} kurallarıyla türetilir.</li>
     * </ul>
     */
    static JsonObject itemsRaw(JsonArray runtimeStates, JsonObject currentItems, JsonObject dump) {
        var dumpItems = dump.getAsJsonObject("items");
        var fuel = dump.getAsJsonObject("fuel");
        var result = new TreeMap<String, JsonObject>();
        var hidden = new TreeMap<String, Integer>();

        for (var element : runtimeStates) {
            var state = element.getAsJsonObject();
            var name = state.get("name").getAsString();
            var id = state.get("id").getAsInt();
            var measured = dumpItems.getAsJsonObject(name);
            var current = currentItems.getAsJsonObject(name);

            if (current != null) {
                var item = current.deepCopy();
                item.addProperty("id", id);
                if (measured != null) {
                    setTags(item, measured.getAsJsonArray("tags"));
                }
                result.put(name, item);
            } else if (measured != null) {
                result.put(name, measuredItem(name, id, measured, fuel.getAsJsonObject(name)));
            } else {
                hidden.put(name, id);
            }
        }

        hidden.forEach((name, id) -> {
            var item = derive(name, result, currentItems);
            item.addProperty("id", id);
            result.put(name, item);
        });

        var output = new JsonObject();
        result.forEach(output::add);
        return output;
    }

    /**
     * BDS'te görünmeyen (elde tutulamayan) yeni eşyanın değerleri. Kurallar bugünkü 26.30 verisinde ölçüldü:
     * <ul>
     *     <li>{@code X_double_slab} = {@code X_slab}: 60 çift yarım blokta birebir.</li>
     *     <li>{@code X_standing_sign}, {@code X_wall_sign}: görünür tabelayla aynı DEĞİL (yığın 64). Değer, görünür
     *     tabelası ({@code Y_sign}) yeni eşyanın görünür tabelasıyla aynı olan mevcut gizli tabelalardan kopyalanır ve
     *     bu grubun hepsi aynı olmalıdır: yanıcı ağaçlarda yakıt 300, Nether ağaçlarında (crimson, warped) 0.</li>
     *     <li>{@code item.X} = {@code X}, yığın 64: mevcut 16 örneğin hepsinde (15'i zaten eşit, {@code item.bed} yığın
     *     farkı).</li>
     *     <li>{@link #DEFAULTED_ITEMS}: varsayılan değerler.</li>
     * </ul>
     */
    static JsonObject derive(String name, Map<String, JsonObject> staged, JsonObject currentItems) {
        if (name.endsWith("_double_slab")) {
            return copyValues(require(staged, name.replace("_double_slab", "_slab")));
        }
        for (var suffix : List.of("_standing_sign", "_wall_sign")) {
            if (name.endsWith(suffix)) {
                var visibleSign = copyValues(require(staged, name.replace(suffix, "_sign")));
                return copyValues(uniformValues(currentItems, suffix, visibleSign));
            }
        }
        if (name.startsWith("minecraft:item.")) {
            var item = copyValues(require(staged, "minecraft:" + name.substring("minecraft:item.".length())));
            item.addProperty("maxStackSize", 64);
            return item;
        }
        if (DEFAULTED_ITEMS.contains(name)) {
            return defaultItem();
        }
        throw new IllegalStateException("BDS'te görünmeyen yeni eşya için kural yok: " + name);
    }

    /**
     * {@code item_components.nbt}: bugünkü girdiler aynen korunur, yalnızca yeni eşyalar eklenir.
     *
     * <p>CloudburstMC ve Altay (PocketMine) bileşen temelli eşyaların çoğunda boş bileşen gönderiyor (ör. ok); Allay'in
     * Endstone verisi daha zengin ve 26.50 istemcileriyle çalışıyor. Mevcut eşyaları değiştirmek davranış riski taşıdığı
     * için değiştirilmez; bugün girdisi olmayan mevcut eşyalara da girdi eklenmez (Allay onlar için boş tanım gönderiyor).
     * Yeni eşyada tek kaynak CloudburstMC; sürüm ve bileşen bayrağı {@code runtime_item_states}'ten.</p>
     */
    static NbtMap itemComponents(NbtMap current, JsonObject currentItems, NbtMap cloudburstComponents, JsonArray runtimeStates) {
        var result = new TreeMap<String, Object>(current);
        for (var element : runtimeStates) {
            var state = element.getAsJsonObject();
            var name = state.get("name").getAsString();
            if (currentItems.has(name)) {
                continue;
            }
            var source = cloudburstComponents.getCompound(name, NbtMap.EMPTY);
            result.put(name, NbtMap.builder()
                    .putCompound("components", source.getCompound("components", NbtMap.EMPTY))
                    .putBoolean("isComponentBased", state.get("componentBased").getAsBoolean())
                    .putInt("version", state.get("version").getAsInt())
                    .build());
        }
        return NbtMap.fromMap(result);
    }

    /**
     * Yaratıcı mod grupları ({@code creative_groups.json}) ve eşyaları ({@code creative_items.nbt}).
     *
     * <p>Endstone blok eşyasına bloğun eski veri değerini {@code damage} olarak yazıyordu (sarkıt taşı 8, ametist
     * kümesi 1); CloudburstMC aynı bilgiyi {@code block_state_b64} ile veriyor ve {@code damage} yazmıyor. Allay bu değeri
     * eşya meta'sı olarak kullandığı için mevcut eşyada bugünkü değer korunur (adı listede tekil olan 29 eşya). 26.50'nin
     * yeni yaratıcı mod eşyalarının hiçbiri varsayılan dışı blok durumunda değil (ölçüldü), onlarda {@code damage} 0'dır.</p>
     */
    static CreativeData creative(JsonObject cloudburstCreative, NbtMap currentCreative) throws IOException {
        var nameCounts = new HashMap<String, Integer>();
        var legacyDamage = new HashMap<String, Integer>();
        for (var item : currentCreative.getList("items", NbtType.COMPOUND)) {
            nameCounts.merge(item.getString("name"), 1, Integer::sum);
            if (item.getShort("damage") != 0 && !item.containsKey("tag")) {
                legacyDamage.put(item.getString("name"), (int) item.getShort("damage"));
            }
        }
        nameCounts.forEach((name, count) -> {
            if (count > 1) {
                legacyDamage.remove(name);
            }
        });

        var groups = new JsonArray();
        var categories = new ArrayList<String>();
        for (var element : cloudburstCreative.getAsJsonArray("groups")) {
            var source = element.getAsJsonObject();
            var category = capitalize(source.get("category").getAsString());
            categories.add(category);
            var group = new JsonObject();
            group.addProperty("category", category);
            // Adsız ayırıcı gruplarda CloudburstMC simgeyi hava yazıyor; Endstone verisinde simge alanı hiç yok.
            var icon = source.getAsJsonObject("icon").get("id").getAsString();
            if (!icon.equals("minecraft:air")) {
                group.addProperty("icon", icon);
            }
            group.addProperty("name", source.get("name").getAsString());
            groups.add(group);
        }

        var items = new ArrayList<NbtMap>();
        for (var element : cloudburstCreative.getAsJsonArray("items")) {
            var source = element.getAsJsonObject();
            var groupIndex = source.get("groupId").getAsInt();
            var name = source.get("id").getAsString();
            var damage = source.has("damage") ? source.get("damage").getAsInt()
                    : source.has("block_state_b64") ? legacyDamage.getOrDefault(name, 0) : 0;
            NbtMapBuilder item = NbtMap.builder()
                    .putString("category", categories.get(groupIndex))
                    // Tipler bugünkü dosyayla aynı: damage short, groupIndex long (yükleyici getLong okuyor).
                    .putShort("damage", (short) damage)
                    .putLong("groupIndex", groupIndex)
                    .putString("name", name);
            if (source.has("nbt_b64")) {
                item.putCompound("tag", DataFiles.readLittleEndianBase64(source.get("nbt_b64").getAsString()));
            }
            items.add(item.build());
        }
        return new CreativeData(groups, NbtMap.builder().putList("items", NbtType.COMPOUND, items).build());
    }

    record CreativeData(JsonArray groups, NbtMap items) {
    }

    private static JsonObject measuredItem(String name, int id, JsonObject measured, JsonObject fuel) {
        if (measured.has("error")) {
            throw new IllegalStateException(name + " dökümde hatalı: " + measured.get("error"));
        }
        if (measured.get("enchantSlots").isJsonArray() && !measured.getAsJsonArray("enchantSlots").isEmpty()) {
            throw new IllegalStateException(name + " büyülenebilir; büyü değeri ölçülemiyor, elle karar gerekli");
        }
        if (fuel == null || fuel.get("capped").getAsBoolean()) {
            throw new IllegalStateException(name + " yakıt süresi ölçülemedi");
        }
        var item = defaultItem();
        item.addProperty("id", id);
        item.addProperty("maxStackSize", measured.get("maxAmount").getAsInt());
        var durability = measured.get("maxDurability");
        if (durability != null && !durability.isJsonNull()) {
            item.addProperty("isDamageable", true);
            item.addProperty("maxDamage", durability.getAsInt());
        }
        var ticks = fuel.get("ticks").getAsInt();
        item.addProperty("furnaceBurnDuration", (double) Math.round((double) ticks / FUEL_STEP_TICKS) * FUEL_STEP_TICKS);
        setTags(item, measured.getAsJsonArray("tags"));
        return item;
    }

    private static JsonObject defaultItem() {
        var item = new JsonObject();
        item.addProperty("armorValue", 0);
        item.addProperty("attackDamage", 0);
        item.addProperty("enchantValue", 0);
        item.addProperty("furnaceBurnDuration", 0.0);
        item.addProperty("furnaceXPMultiplier", 0.0);
        item.addProperty("isDamageable", false);
        item.addProperty("maxDamage", 0);
        item.addProperty("maxStackSize", 64);
        item.addProperty("toughnessValue", 0);
        return item;
    }

    private static void setTags(JsonObject item, JsonArray tags) {
        var sorted = new TreeSet<String>();
        tags.forEach(tag -> sorted.add(BlockDataImport.namespaced(tag.getAsString())));
        item.remove("tags");
        if (!sorted.isEmpty()) {
            var array = new JsonArray();
            sorted.forEach(array::add);
            item.add("tags", array);
        }
    }

    private static JsonObject copyValues(JsonObject source) {
        var item = new JsonObject();
        for (var field : VALUE_FIELDS) {
            item.add(field, source.get(field).deepCopy());
        }
        return item;
    }

    private static JsonObject uniformValues(JsonObject currentItems, String suffix, JsonObject visibleSign) {
        JsonObject reference = null;
        for (var entry : currentItems.entrySet()) {
            var visible = currentItems.getAsJsonObject(entry.getKey().replace(suffix, "_sign"));
            if (!entry.getKey().endsWith(suffix) || visible == null || !copyValues(visible).equals(visibleSign)) {
                continue;
            }
            var values = copyValues(entry.getValue().getAsJsonObject());
            if (reference == null) {
                reference = values;
            } else if (!reference.equals(values)) {
                throw new IllegalStateException(suffix + " sonekli mevcut eşyaların değerleri farklı; kural geçersiz");
            }
        }
        if (reference == null) {
            throw new IllegalStateException(suffix + " için görünür tabelası aynı olan mevcut eşya yok");
        }
        return reference;
    }

    private static JsonObject require(Map<String, JsonObject> staged, String name) {
        var item = staged.get(name);
        if (item == null) {
            throw new IllegalStateException("türetme kaynağı yok: " + name);
        }
        return item;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
