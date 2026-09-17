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
 * Canlı Bedrock verisinin ({@code data/resources}) değişmezleri.
 *
 * <p>Veri Endstone dökümü yerine {@link BedrockDataImporter} ile üretiliyor (yol haritası Adım 5–6). Bu testler her veri
 * güncellemesinde geçerli kalır: resmî palet, BDS kayıt dökümü ({@code unpacked/bds_registry_dump.json}) ve üretilen
 * dosyalar birbiriyle tutarlı olmalı. 26.30 → 26.50 geçişindeki tek tek farklar (fidan yanma değerleri, yahni tarifleri,
 * etiket değişiklikleri, 20 tarifte çıktı veri değeri) git geçmişinde {@code StagedBedrockDataTest} ile doğrulandı.</p>
 */
class BedrockDataTest {

    private static final Path RESOURCES = Path.of("data/resources");
    private static final Path UNPACKED = RESOURCES.resolve("unpacked");
    /** Veri sürümünün resmî paleti; veri güncellenince bu yol da güncellenir. */
    private static final Path OFFICIAL_PALETTE = RESOURCES.resolve("protocol_palettes/1_26_50.nbt");
    private static final Path DATA_DRIVEN_BLOCKS = RESOURCES.resolve("protocol_palettes/1_26_50_data_driven_blocks.nbt");
    private static final int UNKNOWN_HASH = -2;
    private static final int FUEL_STEP_TICKS = 50;

    @Test
    void blockPaletteAndPhysicsMatchOfficialPalette() throws IOException {
        var official = stateHashes(readPalette(OFFICIAL_PALETTE));
        assertEquals(official, stateHashes(readPalette(UNPACKED.resolve("block_palette.nbt"))));

        // minecraft:unknown Allay verisinde hesaplanmış hash yerine -2 ile yazılır.
        official.remove(stateHash(NbtMap.builder().putString("name", "minecraft:unknown").putCompound("states", NbtMap.EMPTY).build()));
        official.add(UNKNOWN_HASH);
        var physics = new HashSet<Integer>();
        readJson(UNPACKED.resolve("block_states_raw.json")).getAsJsonArray()
                .forEach(state -> physics.add((int) state.getAsJsonObject().get("blockStateHash").getAsLong()));
        assertEquals(official, physics);
    }

    @Test
    void blockTypesMatchVanillaDefaultsAndTags() throws IOException {
        var statesByHash = new HashMap<Integer, NbtMap>();
        var paletteTypes = new TreeSet<String>();
        for (var state : readPalette(UNPACKED.resolve("block_palette.nbt"))) {
            statesByHash.put(stateHash(state), state.getCompound("states"));
            paletteTypes.add(state.getString("name"));
        }
        var blockTypes = readJson(RESOURCES.resolve("block_types.json")).getAsJsonObject();
        assertEquals(paletteTypes, blockTypes.keySet());

        var dump = readJson(UNPACKED.resolve("bds_registry_dump.json")).getAsJsonObject().getAsJsonObject("blocks");
        for (var name : paletteTypes) {
            var type = blockTypes.getAsJsonObject(name);
            var defaultHash = (int) type.get("defaultBlockStateHash").getAsLong();
            if (name.equals("minecraft:unknown")) {
                assertEquals(UNKNOWN_HASH, defaultHash);
                continue;
            }
            var defaultStates = statesByHash.get(defaultHash);
            assertNotNull(defaultStates, name + " varsayılan durumu palette yok");
            var vanilla = dump.getAsJsonObject(name);
            for (var key : defaultStates.keySet()) {
                assertEquals(paletteValue(vanilla.getAsJsonObject("states").get(key)), String.valueOf(defaultStates.get(key)),
                        name + " varsayılan durumu BDS'le farklı: " + key);
            }
            var vanillaTags = new TreeSet<String>();
            vanilla.getAsJsonArray("tags").forEach(tag -> vanillaTags.add(BlockDataImport.namespaced(tag.getAsString())));
            assertEquals(vanillaTags, strings(type.getAsJsonArray("tags")), name + " etiketleri");
        }
    }

    @Test
    void itemValuesMatchVanillaMeasurements() throws IOException {
        var items = readJson(UNPACKED.resolve("items_raw.json")).getAsJsonObject();
        var ids = new HashSet<Integer>();
        items.entrySet().forEach(entry ->
                assertTrue(ids.add(entry.getValue().getAsJsonObject().get("id").getAsInt()), "yinelenen kimlik: " + entry.getKey()));

        var dump = readJson(UNPACKED.resolve("bds_registry_dump.json")).getAsJsonObject();
        var measuredItems = dump.getAsJsonObject("items");
        var fuel = dump.getAsJsonObject("fuel");
        for (var entry : items.entrySet()) {
            var measured = measuredItems.getAsJsonObject(entry.getKey());
            if (measured == null) {
                // BDS'te elde tutulamayan eşya; değerleri türetme kurallarından (ItemDataImport#derive).
                continue;
            }
            var item = entry.getValue().getAsJsonObject();
            var name = entry.getKey();
            assertEquals(measured.get("maxAmount").getAsInt(), item.get("maxStackSize").getAsInt(), name + " yığın");
            var durability = measured.get("maxDurability");
            assertEquals(durability.isJsonNull() ? 0 : durability.getAsInt(), item.get("maxDamage").getAsInt(), name + " dayanıklılık");
            var burn = fuel.getAsJsonObject(name);
            if (!burn.get("capped").getAsBoolean()) {
                assertEquals(Math.round(burn.get("ticks").getAsInt() / (double) FUEL_STEP_TICKS) * FUEL_STEP_TICKS,
                        item.get("furnaceBurnDuration").getAsDouble(), name + " yakıt");
            }
        }
    }

    @Test
    void tagFilesAreAggregatedFromEntries() throws IOException {
        assertEquals(aggregateTags(readJson(RESOURCES.resolve("block_types.json")).getAsJsonObject()),
                sortedTagFile(readJson(UNPACKED.resolve("block_tags.json")).getAsJsonObject()));
        assertEquals(aggregateTags(readJson(UNPACKED.resolve("items_raw.json")).getAsJsonObject()),
                sortedTagFile(readJson(UNPACKED.resolve("item_tags.json")).getAsJsonObject()));
    }

    @Test
    void dataDrivenBlockDefinitionsExistInBlockData() throws IOException {
        var blockTypes = readJson(RESOURCES.resolve("block_types.json")).getAsJsonObject();
        var definitions = readNbt(DATA_DRIVEN_BLOCKS);
        assertFalse(definitions.isEmpty());
        definitions.keySet().forEach(name -> assertTrue(blockTypes.has(name), name + " block_types.json'da yok"));
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

    private static String paletteValue(JsonElement value) {
        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? "1" : "0";
        }
        return primitive.isNumber() ? String.valueOf(primitive.getAsInt()) : primitive.getAsString();
    }

    /**
     * Yaratıcı menüdeki blok eşyası veri değeri ({@code damage}) taşımaz. 26.50'de BDS bu değeri ne yaratıcı menüde ne
     * tariflerin çıktısında gönderiyor (yalnızca renkli yatağın 16 girdisi damage taşıyor) ve Allay kırılan bloktan meta 0
     * düşürüyor. Eski veriden kalan değer menüden alınan eşyayı üretilen ve yerden toplanan eşyadan ayırır, yığılmazlar
     * (26.50 geçişinde sandık 2, piston 1, dağıtıcı 3 kalmıştı).
     */
    @Test
    void creativeBlockItemsCarryNoLegacyDamage() throws IOException {
        var blockTypes = readJson(RESOURCES.resolve("block_types.json")).getAsJsonObject().keySet();
        var items = readNbt(RESOURCES.resolve("creative_items.nbt")).getList("items", NbtType.COMPOUND);
        var entryCounts = new HashMap<String, Integer>();
        items.forEach(item -> entryCounts.merge(item.getString("name"), 1, Integer::sum));

        var withDamage = new TreeMap<String, Integer>();
        for (var item : items) {
            var name = item.getString("name");
            if (blockTypes.contains(name) && entryCounts.get(name) == 1 && item.getShort("damage") != 0) {
                withDamage.put(name, (int) item.getShort("damage"));
            }
        }
        assertEquals(Map.of(), withDamage);
    }

    private static Set<String> strings(JsonArray array) {
        var result = new TreeSet<String>();
        if (array != null) {
            array.forEach(element -> result.add(element.getAsString()));
        }
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
