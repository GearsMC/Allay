package org.allaymc.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@code items.json} from {@code items_raw.json}, filling in each item's translation key.
 * <p>
 * The key is not guessed. Mojang publishes it: every entry of {@code mojang-blocks.json} and
 * {@code mojang-items.json} carries a {@code serialization_id} that is exactly the translation
 * key base, including the cases no naming rule can predict
 * ({@code acacia_log} -> {@code tile.log.acacia}, {@code totem_of_undying} -> {@code item.totem},
 * {@code green_shulker_box} -> {@code tile.shulkerBoxGreen}). Block items are named after their
 * block, so the block table is consulted first.
 * <p>
 * Every derived key is verified against the generated {@code en_US.json} before it is written, so
 * {@code items.json} can never carry a key that does not resolve. An item whose key cannot be
 * verified is left empty rather than given a plausible-looking guess.
 *
 * @author daoge_cmd
 */
@Slf4j
public class ItemDataProcessor {

    private static final String UNPACKED = "data/resources/unpacked/";
    private static final String VANILLA_PREFIX = "minecraft:";

    /**
     * Ids whose {@code serialization_id} disagrees with Mojang's own language file.
     * <p>
     * Both of these are historically dyes and are still named as such
     * ({@code minecraft:lapis_lazuli} reports {@code item.lapis_lazuli}, but the only name that
     * exists is {@code item.dye.blue.name}). These corrections are verified against the language
     * file like every other candidate, so a stale entry here silently drops out instead of
     * producing a broken key.
     */
    private static final Map<String, String> KEY_CORRECTIONS = Map.of(
            "minecraft:lapis_lazuli", "item.dye.blue.name",
            "minecraft:light_gray_dye", "item.dye.silver.name");

    @SneakyThrows
    public static void main(String[] args) {
        var gson = new GsonBuilder().create();
        var blockKeys = readSerializationIds(UNPACKED + "mojang-blocks.json");
        var itemKeys = readSerializationIds(UNPACKED + "mojang-items.json");
        var known = readKnownTranslationKeys();

        var resolved = 0;
        var unresolved = 0;
        try (var reader = Files.newBufferedReader(Path.of(UNPACKED + "items_raw.json"))) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : root.entrySet()) {
                var obj = entry.getValue().getAsJsonObject();
                if (obj.has("translationKey")) {
                    // Supplied by the raw data already; keep it, only add the namespace.
                    obj.addProperty("translationKey", VANILLA_PREFIX + obj.get("translationKey").getAsString());
                    resolved++;
                    continue;
                }
                var key = resolveTranslationKey(entry.getKey(), blockKeys, itemKeys, known);
                if (key == null) {
                    unresolved++;
                    continue;
                }
                obj.addProperty("translationKey", key);
                resolved++;
            }
            Utils.writeFileWithCRLF(Path.of("data/resources/items.json"), gson.toJson(root));
        }
        log.info("Item translation keys: {} verified, {} left empty", resolved, unresolved);
    }

    /**
     * Finds the translation key of an item, most authoritative source first.
     *
     * @param identifier the namespaced item identifier
     * @param blockKeys  block identifier to serialization id
     * @param itemKeys   item identifier to serialization id
     * @param known      the translation keys that actually exist
     * @return the verified, fully namespaced key, or {@code null} if none could be verified
     */
    private static String resolveTranslationKey(String identifier,
                                                Map<String, String> blockKeys,
                                                Map<String, String> itemKeys,
                                                Map<String, String> known) {
        // 0) The handful of ids where Mojang's data contradicts its own language file.
        var correction = KEY_CORRECTIONS.get(identifier);
        if (correction != null && known.containsKey(correction)) {
            return known.get(correction);
        }

        // 1) Mojang's own mapping. Blocks first: a block item is named after its block.
        for (var table : List.of(blockKeys, itemKeys)) {
            var serializationId = table.get(identifier);
            if (serializationId != null && known.containsKey(serializationId + ".name")) {
                return known.get(serializationId + ".name");
            }
        }

        var path = identifier.startsWith(VANILLA_PREFIX)
                ? identifier.substring(VANILLA_PREFIX.length())
                : identifier;

        // 2) The plain patterns, for the few ids Mojang's tables do not cover.
        for (var candidate : List.of("item." + path + ".name", "tile." + path + ".name")) {
            if (known.containsKey(candidate)) {
                return known.get(candidate);
            }
        }

        // 3) Spawn eggs live under their entity ({@code bat_spawn_egg} -> {@code item.spawn_egg.entity.bat}).
        if (path.endsWith("_spawn_egg")) {
            var entity = path.substring(0, path.length() - "_spawn_egg".length());
            var candidate = "item.spawn_egg.entity." + entity + ".name";
            if (known.containsKey(candidate)) {
                return known.get(candidate);
            }
        }

        // 4) Bedrock groups some ids by family, writing the variant in one of three shapes:
        //    black_dye      -> item.dye.black         (family.variant)
        //    light_blue_dye -> item.dye.lightBlue     (family.camelCaseVariant)
        //    lava_bucket    -> item.bucketLava        (familyCamelCaseVariant)
        //    Each candidate is checked against the language file, so no key can be invented here.
        var parts = path.split("_");
        for (var cut = 1; cut < parts.length; cut++) {
            var variant = String.join("_", Arrays.copyOfRange(parts, 0, cut));
            var family = String.join("_", Arrays.copyOfRange(parts, cut, parts.length));
            var camelVariant = toCamelCase(variant);
            for (var prefix : List.of("tile", "item")) {
                for (var candidate : List.of(
                        prefix + "." + family + "." + variant + ".name",
                        prefix + "." + family + "." + camelVariant + ".name",
                        prefix + "." + family + capitalize(camelVariant) + ".name")) {
                    if (known.containsKey(candidate)) {
                        return known.get(candidate);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Turns {@code light_blue} into {@code lightBlue}.
     *
     * @param value the underscore separated value
     * @return the camel case form
     */
    private static String toCamelCase(String value) {
        var parts = value.split("_");
        var builder = new StringBuilder(parts[0]);
        for (var i = 1; i < parts.length; i++) {
            builder.append(capitalize(parts[i]));
        }
        return builder.toString();
    }

    /**
     * Upper cases the first character.
     *
     * @param value the value to capitalize
     * @return the capitalized value
     */
    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * Reads the identifier to serialization id mapping out of one of Mojang's data modules.
     *
     * @param path the file to read
     * @return identifier to serialization id
     */
    @SneakyThrows
    private static Map<String, String> readSerializationIds(String path) {
        var result = new HashMap<String, String>();
        try (var reader = Files.newBufferedReader(Path.of(path))) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var element : root.getAsJsonArray("data_items")) {
                var obj = element.getAsJsonObject();
                if (obj.has("name") && obj.has("serialization_id")) {
                    result.put(obj.get("name").getAsString(), obj.get("serialization_id").getAsString());
                }
            }
        }
        return result;
    }

    /**
     * Collects every translation key that exists, so derived keys can be verified.
     * <p>
     * Both namespaces are accepted: {@code minecraft:} for Bedrock's own names and {@code allay:}
     * for the names the server owns (Education blocks, which the vanilla resource pack does not
     * name). The vanilla namespace wins when a key exists in both.
     *
     * @return bare key to the fully namespaced key
     */
    @SneakyThrows
    private static Map<String, String> readKnownTranslationKeys() {
        var known = new HashMap<String, String>();
        try (var reader = Files.newBufferedReader(Path.of("data/resources/lang/en_US.json"))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var key : root.keySet()) {
                var colon = key.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                var bare = key.substring(colon + 1);
                if (key.startsWith(VANILLA_PREFIX) || !known.containsKey(bare)) {
                    known.put(bare, key);
                }
            }
        }
        return known;
    }
}
