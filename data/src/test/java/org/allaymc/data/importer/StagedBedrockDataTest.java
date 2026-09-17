package org.allaymc.data.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.allaymc.api.utils.hash.HashUtils;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 26.50 veri setinin doğrulaması (yol haritası Adım 5.3).
 *
 * <p>Dosyalar {@link BedrockDataImporter} ile herkese açık kaynaklardan üretilir ve henüz sunucu tarafından okunmaz.
 * Testler yalnızca depodaki dosyalara bakar (ağ yok, CI'da koşar): resmî 26.50 paleti
 * ({@code protocol_palettes/1_26_50.nbt}), bugünkü 26.30 Endstone verisi ve BDS kayıt dökümü. Beklenen farklar burada
 * adıyla listelenir; listede olmayan her fark testi düşürür.</p>
 */
class StagedBedrockDataTest {

    private static final Path RESOURCES = Path.of("data/resources");
    private static final Path UNPACKED = RESOURCES.resolve("unpacked");
    private static final Path STAGING = UNPACKED.resolve("staging-1.26.50");
    private static final Path OFFICIAL_PALETTE = RESOURCES.resolve("protocol_palettes/1_26_50.nbt");

    /**
     * 26.50 fizik verisinde bugünkü 26.30 verisinden farklı çıkan alanlar (CloudburstMC/Data {@code blocks.json}). Kiraz
     * ve soluk meşe fidanı diğer fidanlar gibi yanmaz oldu (20/5 → 0/0); bambu fidanı değişmedi.
     * {@code liquidClipShape} karşılaştırılmaz: iki tarafta da ilklenmemiş bellekten gelen anlamsız sayılar var.
     */
    private static final Map<String, Set<String>> KNOWN_PHYSICS_CHANGES = Map.of(
            "minecraft:cherry_sapling", Set.of("burnOdds", "flameOdds"),
            "minecraft:pale_oak_sapling", Set.of("burnOdds", "flameOdds")
    );

    /** 26.50'nin mevcut eşyalara eklediği etiketler (BDS dökümü). */
    private static final Map<String, Set<String>> KNOWN_ITEM_TAG_ADDITIONS = Map.of(
            "minecraft:brown_mushroom", Set.of("minecraft:mushrooms_for_stew"),
            "minecraft:red_mushroom", Set.of("minecraft:mushrooms_for_stew")
    );

    /** 26.50'nin mevcut eşyalardan kaldırdığı etiketler (BDS dökümü). */
    private static final Map<String, Set<String>> KNOWN_ITEM_TAG_REMOVALS = Map.of(
            "minecraft:observer", Set.of("minecraft:sulfur_cube_archetype_slow_bouncy"),
            "minecraft:redstone_lamp", Set.of("minecraft:sulfur_cube_archetype_slow_bouncy")
    );

    /**
     * Endstone çıktı eşyasına bloğun eski veri değerini yazıyordu (sandık 2, dağıtıcı 3). BDS'in
     * istemciye gönderdiği tarifte bu değer yok; üretilen tarifte çıktı {@code data} taşımaz, geri kalanı aynıdır.
     * Oyun içi doğrulama Adım 6'da: üretilen sandık yerden alınan sandıkla aynı yığına girmeli.
     */
    private static final Set<String> KNOWN_RECIPE_OUTPUT_DATA_REMOVALS = Set.of(
            "minecraft:Chest_recipeId", "minecraft:chest_from_crimson_planks", "minecraft:chest_from_mangrove_planks",
            "minecraft:chest_from_warped_planks", "minecraft:copper_chest", "minecraft:dispenser", "minecraft:dropper",
            "minecraft:ender_chest", "minecraft:piston", "minecraft:piston_from_crimson_planks",
            "minecraft:piston_from_mangrove_planks", "minecraft:piston_from_warped_planks", "minecraft:sticky_piston",
            "minecraft:stonecutter", "minecraft:trapped_chest", "minecraft:waxing_copper_chest",
            "minecraft:waxing_exposed_copper_chest", "minecraft:waxing_oxidized_copper_chest",
            "minecraft:waxing_weathered_copper_chest"
    );

    /** 26.50'de mantar yahnisi ve şüpheli yahniler iki ayrı mantar yerine {@code mushrooms_for_stew} etiketini kullanıyor. */
    private static final String STEW_TAG = "minecraft:mushrooms_for_stew";

    /** 26.50'nin mevcut blok türlerine eklediği etiketler (BDS dökümü). */
    private static final Set<String> KNOWN_NEW_BLOCK_TAGS = Set.of("minecraft:cornerable_stairs", "minecraft:leaves");

    @Test
    void blockPaletteMatchesOfficialPalette() throws IOException {
        var staged = stateHashes(readPalette(STAGING.resolve("unpacked/block_palette.nbt")));
        var official = stateHashes(readPalette(OFFICIAL_PALETTE));

        assertEquals(22091, official.size());
        assertEquals(official, staged);
    }

    @Test
    void blockPhysicsCoversPaletteAndKeepsUnchangedStates() throws IOException {
        // minecraft:unknown Allay verisinde hesaplanmış hash yerine -2 ile yazılır (bugünkü dosyada da öyle).
        var official = stateHashes(readPalette(OFFICIAL_PALETTE));
        official.remove(stateHash(NbtMap.builder().putString("name", "minecraft:unknown").putCompound("states", NbtMap.EMPTY).build()));
        official.add(-2);
        var staged = byStateHash(readJson(STAGING.resolve("unpacked/block_states_raw.json")).getAsJsonArray());
        assertEquals(official, staged.keySet());
        assertEquals("minecraft:unknown", staged.get(-2).get("name").getAsString());

        var current = byStateHash(readJson(UNPACKED.resolve("block_states_raw.json")).getAsJsonArray());
        var unexpected = new TreeSet<String>();
        var kept = 0;
        for (var entry : current.entrySet()) {
            var stagedState = staged.get(entry.getKey());
            if (stagedState == null) {
                // 26.50'de durum kümesi değişen 121 tür; bu durumlar artık yok.
                continue;
            }
            kept++;
            var name = entry.getValue().get("name").getAsString();
            for (var field : entry.getValue().keySet()) {
                if (field.equals("liquidClipShape")) {
                    continue;
                }
                if (!entry.getValue().get(field).equals(stagedState.get(field))
                    && !KNOWN_PHYSICS_CHANGES.getOrDefault(name, Set.of()).contains(field)) {
                    unexpected.add(name + " " + field);
                }
            }
        }
        assertEquals(16329, kept);
        assertEquals(Set.of(), unexpected);
    }

    @Test
    void blockTypesUseVanillaDefaultsAndKeepCurrentData() throws IOException {
        var palette = readPalette(STAGING.resolve("unpacked/block_palette.nbt"));
        var statesByHash = new HashMap<Integer, NbtMap>();
        var paletteTypes = new TreeSet<String>();
        for (var state : palette) {
            statesByHash.put(stateHash(state), state.getCompound("states"));
            paletteTypes.add(state.getString("name"));
        }
        var staged = readJson(STAGING.resolve("resources/block_types.json")).getAsJsonObject();
        assertEquals(paletteTypes, staged.keySet());

        var dump = readJson(STAGING.resolve("bds_registry_dump.json")).getAsJsonObject().getAsJsonObject("blocks");
        var current = readJson(RESOURCES.resolve("block_types.json")).getAsJsonObject();
        for (var name : paletteTypes) {
            var type = staged.getAsJsonObject(name);
            var defaultHash = (int) type.get("defaultBlockStateHash").getAsLong();
            if (name.equals("minecraft:unknown")) {
                // Allay bilinmeyen bloğu ağda -2 ile temsil eder; bugünkü değer korunur.
                assertEquals(current.getAsJsonObject(name), type);
                continue;
            }
            var defaultStates = statesByHash.get(defaultHash);
            assertNotNull(defaultStates, name + " varsayılan durumu palette yok");

            var vanillaStates = dump.getAsJsonObject(name).getAsJsonObject("states");
            for (var key : defaultStates.keySet()) {
                assertEquals(normalize(vanillaStates.get(key)), String.valueOf(defaultStates.get(key)),
                        name + " varsayılan durumu BDS'le farklı: " + key);
            }

            var currentType = current.getAsJsonObject(name);
            if (currentType == null) {
                continue;
            }
            var currentTags = strings(currentType.getAsJsonArray("tags"));
            var stagedTags = strings(type.getAsJsonArray("tags"));
            assertTrue(stagedTags.containsAll(currentTags), name + " etiket kaybetti");
            var added = new TreeSet<>(stagedTags);
            added.removeAll(currentTags);
            assertTrue(KNOWN_NEW_BLOCK_TAGS.containsAll(added), name + " beklenmeyen etiket: " + added);
        }
    }

    @Test
    void itemsKeepCurrentValuesAndCoverEveryItem() throws IOException {
        var staged = readJson(STAGING.resolve("unpacked/items_raw.json")).getAsJsonObject();
        assertEquals(2076, staged.size());

        var ids = new HashSet<Integer>();
        for (var entry : staged.entrySet()) {
            assertTrue(ids.add(entry.getValue().getAsJsonObject().get("id").getAsInt()), "yinelenen kimlik: " + entry.getKey());
        }

        var current = readJson(UNPACKED.resolve("items_raw.json")).getAsJsonObject();
        for (var entry : current.entrySet()) {
            var name = entry.getKey();
            var stagedItem = staged.getAsJsonObject(name);
            assertNotNull(stagedItem, name + " kayboldu");
            for (var field : entry.getValue().getAsJsonObject().keySet()) {
                if (field.equals("id") || field.equals("tags")) {
                    continue;
                }
                assertEquals(entry.getValue().getAsJsonObject().get(field), stagedItem.get(field), name + " " + field);
            }
            var currentTags = strings(entry.getValue().getAsJsonObject().getAsJsonArray("tags"));
            var stagedTags = strings(stagedItem.getAsJsonArray("tags"));
            var added = new TreeSet<>(stagedTags);
            added.removeAll(currentTags);
            var removed = new TreeSet<>(currentTags);
            removed.removeAll(stagedTags);
            assertEquals(KNOWN_ITEM_TAG_ADDITIONS.getOrDefault(name, Set.of()), added, name + " eklenen etiketler");
            assertEquals(KNOWN_ITEM_TAG_REMOVALS.getOrDefault(name, Set.of()), removed, name + " kaldırılan etiketler");
        }

        // Yeni ve BDS'te görünen eşyaların değerleri dökümden gelir.
        var dump = readJson(STAGING.resolve("bds_registry_dump.json")).getAsJsonObject();
        for (var entry : staged.entrySet()) {
            var name = entry.getKey();
            var measured = dump.getAsJsonObject("items").getAsJsonObject(name);
            if (current.has(name) || measured == null) {
                continue;
            }
            var item = entry.getValue().getAsJsonObject();
            assertEquals(measured.get("maxAmount").getAsInt(), item.get("maxStackSize").getAsInt(), name + " yığın");
            var fuel = dump.getAsJsonObject("fuel").getAsJsonObject(name);
            assertEquals(Math.round(fuel.get("ticks").getAsInt() / 50.0) * 50.0, item.get("furnaceBurnDuration").getAsDouble(), name + " yakıt");
        }
    }

    /**
     * Mevcut eşyaların istemciye giden bileşen verisi değişmez; yalnızca 26.50'nin yeni eşyaları eklenir. CloudburstMC ve
     * Altay mevcut bileşen temelli eşyaların çoğunda boş bileşen taşıyor (ör. ok), Endstone verisi daha zengin.
     */
    @Test
    void itemComponentsKeepCurrentEntriesAndAddNewItems() throws IOException {
        var current = readNbt(RESOURCES.resolve("item_components.nbt"));
        var staged = readNbt(STAGING.resolve("resources/item_components.nbt"));
        current.forEach((name, tag) -> assertEquals(tag, staged.get(name), name));

        var currentItems = readJson(UNPACKED.resolve("items_raw.json")).getAsJsonObject().keySet();
        var newItems = new TreeSet<>(readJson(STAGING.resolve("unpacked/items_raw.json")).getAsJsonObject().keySet());
        newItems.removeAll(currentItems);
        assertEquals(143, newItems.size());

        var added = new TreeSet<>(staged.keySet());
        added.removeAll(current.keySet());
        assertEquals(newItems, added);
    }

    @Test
    void recipesKeepEveryCurrentRecipe() throws IOException {
        var staged = readJson(STAGING.resolve("resources/recipes.json")).getAsJsonObject();
        var current = readJson(RESOURCES.resolve("recipes.json")).getAsJsonObject();
        var outputDataRemovals = new TreeSet<String>();
        var stewChanges = new TreeSet<String>();
        for (var group : current.keySet()) {
            if (!current.get(group).isJsonArray()) {
                continue;
            }
            var stagedRecipes = new HashSet<JsonElement>();
            staged.getAsJsonArray(group).forEach(recipe -> stagedRecipes.add(withoutNetworkIds(recipe)));
            var missing = new ArrayList<String>();
            for (var element : current.getAsJsonArray(group)) {
                var recipe = withoutNetworkIds(element);
                if (stagedRecipes.contains(recipe)) {
                    continue;
                }
                var id = recipe.getAsJsonObject().has("id") ? recipe.getAsJsonObject().get("id").getAsString() : recipe.toString();
                if (KNOWN_RECIPE_OUTPUT_DATA_REMOVALS.contains(id) && stagedRecipes.contains(withoutOutputData(recipe))) {
                    outputDataRemovals.add(id);
                } else if (id.contains("_stew") && stagedRecipes.contains(withStewTag(recipe))) {
                    stewChanges.add(id);
                } else {
                    missing.add(id);
                }
            }
            assertEquals(List.of(), missing, group + " grubunda bugünkü tarifler eksik ya da farklı");
        }
        assertEquals(KNOWN_RECIPE_OUTPUT_DATA_REMOVALS, outputDataRemovals);
        assertEquals(18, stewChanges.size());
    }

    @Test
    void creativeItemsKeepCurrentEntriesAndTagsAreAggregated() throws IOException {
        var currentGroups = readJson(RESOURCES.resolve("creative_groups.json")).getAsJsonArray();
        var stagedGroups = readJson(STAGING.resolve("resources/creative_groups.json")).getAsJsonArray();
        var stagedGroupSet = new HashSet<JsonElement>();
        stagedGroups.forEach(stagedGroupSet::add);
        currentGroups.forEach(group -> assertTrue(stagedGroupSet.contains(group), "grup eksik: " + group));

        var stagedItems = new HashSet<NbtMap>();
        readNbt(STAGING.resolve("resources/creative_items.nbt")).getList("items", NbtType.COMPOUND)
                .forEach(item -> stagedItems.add(withoutGroupIndex(item)));
        for (var item : readNbt(RESOURCES.resolve("creative_items.nbt")).getList("items", NbtType.COMPOUND)) {
            assertTrue(stagedItems.contains(withoutGroupIndex(item)), "yaratıcı eşya eksik: " + item);
        }

        // Etiket dosyaları tür/eşya başına etiketlerin toplamıdır (Endstone'da da öyleydi); tek doğruluk kaynağı.
        assertEquals(aggregateTags(readJson(STAGING.resolve("resources/block_types.json")).getAsJsonObject()),
                readJson(STAGING.resolve("unpacked/block_tags.json")));
        assertEquals(aggregateTags(readJson(STAGING.resolve("unpacked/items_raw.json")).getAsJsonObject()),
                readJson(STAGING.resolve("unpacked/item_tags.json")));
        // Kuralın dayanağı: bugünkü Endstone dosyaları da tam olarak bu toplamdı.
        assertEquals(aggregateTags(readJson(RESOURCES.resolve("block_types.json")).getAsJsonObject()),
                sortedTagFile(readJson(UNPACKED.resolve("block_tags.json")).getAsJsonObject()));
        assertEquals(aggregateTags(readJson(UNPACKED.resolve("items_raw.json")).getAsJsonObject()),
                sortedTagFile(readJson(UNPACKED.resolve("item_tags.json")).getAsJsonObject()));
    }

    private static JsonObject aggregateTags(JsonObject entries) {
        var members = new TreeMap<String, TreeSet<String>>();
        entries.entrySet().forEach(entry -> strings(entry.getValue().getAsJsonObject().getAsJsonArray("tags"))
                .forEach(tag -> members.computeIfAbsent(tag, k -> new TreeSet<>()).add(entry.getKey())));
        var result = new JsonObject();
        members.forEach((tag, names) -> {
            var array = new JsonArray();
            names.forEach(array::add);
            result.add(tag, array);
        });
        return result;
    }

    private static JsonObject sortedTagFile(JsonObject tagFile) {
        var result = new JsonObject();
        new TreeMap<>(tagFile.asMap()).forEach((tag, names) -> {
            var array = new JsonArray();
            strings(names.getAsJsonArray()).forEach(array::add);
            result.add(tag, array);
        });
        return result;
    }

    private static JsonElement withoutNetworkIds(JsonElement recipe) {
        if (!recipe.isJsonObject()) {
            return recipe;
        }
        var copy = recipe.getAsJsonObject().deepCopy();
        copy.remove("netId");
        copy.remove("uuid");
        return copy;
    }

    private static JsonElement withoutOutputData(JsonElement recipe) {
        var copy = recipe.getAsJsonObject().deepCopy();
        copy.getAsJsonArray("output").forEach(output -> output.getAsJsonObject().remove("data"));
        return copy;
    }

    /** Yahni tarifinde kahverengi ve kırmızı mantar girdileri {@link #STEW_TAG} etiketine dönüşür. */
    private static JsonElement withStewTag(JsonElement recipe) {
        var copy = recipe.getAsJsonObject().deepCopy();
        for (var input : copy.getAsJsonArray("input")) {
            var descriptor = input.getAsJsonObject();
            var item = descriptor.has("item") ? descriptor.get("item").getAsString() : "";
            if (item.equals("minecraft:brown_mushroom") || item.equals("minecraft:red_mushroom")) {
                descriptor.remove("item");
                descriptor.addProperty("tag", STEW_TAG);
            }
        }
        return copy;
    }

    private static NbtMap withoutGroupIndex(NbtMap item) {
        var builder = item.toBuilder();
        builder.remove("groupIndex");
        return builder.build();
    }

    private static String normalize(JsonElement value) {
        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? "1" : "0";
        }
        return primitive.isNumber() ? String.valueOf(primitive.getAsInt()) : primitive.getAsString();
    }

    private static Set<String> strings(JsonArray array) {
        var result = new TreeSet<String>();
        if (array != null) {
            array.forEach(element -> result.add(element.getAsString()));
        }
        return result;
    }

    private static Map<Integer, JsonObject> byStateHash(JsonArray states) {
        var result = new HashMap<Integer, JsonObject>();
        states.forEach(state -> result.put((int) state.getAsJsonObject().get("blockStateHash").getAsLong(), state.getAsJsonObject()));
        return result;
    }

    private static Set<Integer> stateHashes(List<NbtMap> palette) {
        var result = new HashSet<Integer>();
        palette.forEach(state -> result.add(stateHash(state)));
        return result;
    }

    private static int stateHash(NbtMap state) {
        return HashUtils.fnv1a_32_nbt(NbtMap.builder()
                .putString("name", state.getString("name"))
                .putCompound("states", NbtMap.fromMap(new TreeMap<>(state.getCompound("states"))))
                .build());
    }

    private static List<NbtMap> readPalette(Path path) throws IOException {
        return readNbt(path).getList("blocks", NbtType.COMPOUND);
    }

    private static NbtMap readNbt(Path path) throws IOException {
        try (var reader = NbtUtils.createGZIPReader(new BufferedInputStream(Files.newInputStream(path)))) {
            return (NbtMap) reader.readTag();
        }
    }

    private static JsonElement readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }
}
