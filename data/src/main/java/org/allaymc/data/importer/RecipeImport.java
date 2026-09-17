package org.allaymc.data.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CloudburstMC/Data {@code recipes.json}'u (BDS'in {@code CraftingDataPacket}'i) Allay'in Endstone tarif biçimine
 * çevirir.
 *
 * <p>Dönüşüm kuralları {@code StagedBedrockDataTest#recipesKeepEveryCurrentRecipe} ile doğrulanır: bugünkü her tarif,
 * ağ kimlikleri ({@code netId}, {@code uuid}) dışında birebir üretilmelidir. {@code netId}/{@code uuid} Allay'in tarif
 * yükleyicisinde okunmuyor, bu yüzden yazılmaz.</p>
 */
@UtilityClass
final class RecipeImport {

    private static final int ANY_AUX = 32767;
    private static final String TRIM_RECIPE_ID = "minecraft:smithing_armor_trim";

    /**
     * Pişirme tarifleri. CloudburstMC (BDS'in istemciye gönderdiği veri) bunlarda sıra numarası gibi öncelik taşıyor;
     * Endstone girdisi etiket olan tarifte -1, eşya olanda 0 yazıyordu (bugünkü 220 pişirme tarifinin hepsinde). Bugünkü
     * değer korunur.
     */
    private static final Set<String> COOKING_TAGS = Set.of("furnace", "blast_furnace", "smoker", "campfire", "soul_campfire");

    /** Bugünkü dosyada da boş olan gruplar; Endstone bunları {@code null} yazıyordu. */
    private static final List<String> EMPTY_GROUPS = List.of(
            "furnace", "furnaceAux", "materialReducer", "shapedChemistry", "shapelessChemistry"
    );

    /**
     * @param cloudburst     CloudburstMC {@code recipes.json}
     * @param currentRecipes bugünkü {@code recipes.json}; {@code multi} tarifleri CloudburstMC'de yalnızca uuid taşıdığı
     *                       için kimlik ve etiket buradan uuid ile alınır
     */
    static JsonObject recipes(JsonObject cloudburst, JsonObject currentRecipes) throws IOException {
        var groups = new HashMap<String, JsonArray>();
        for (var name : List.of("shapeless", "shaped", "multi", "userDataShapeless", "smithingTransform", "smithingTrim")) {
            groups.put(name, new JsonArray());
        }

        var multiByUuid = new HashMap<String, JsonObject>();
        currentRecipes.getAsJsonArray("multi").forEach(recipe ->
                multiByUuid.put(recipe.getAsJsonObject().get("uuid").getAsString(), recipe.getAsJsonObject()));

        var trimCount = 0;
        for (var element : cloudburst.getAsJsonArray("recipes")) {
            var source = element.getAsJsonObject();
            switch (source.get("type").getAsInt()) {
                case 0 -> groups.get("shapeless").add(shapeless(source));
                case 1 -> groups.get("shaped").add(shaped(source));
                case 4 -> {
                    var uuid = source.get("uuid").getAsString();
                    var current = multiByUuid.get(uuid);
                    if (current == null) {
                        throw new IllegalStateException("yeni çoklu tarif (" + uuid + "): kimliği ve etiketi elle eklenmeli");
                    }
                    groups.get("multi").add(current.deepCopy());
                }
                case 5 -> groups.get("userDataShapeless").add(shapeless(source));
                case 8 -> groups.get("smithingTransform").add(smithingTransform(source));
                case 9 -> {
                    if (++trimCount > 1) {
                        throw new IllegalStateException("birden fazla süsleme tarifi: kimlik kuralı geçersiz");
                    }
                    groups.get("smithingTrim").add(smithingTrim(source));
                }
                default -> throw new IllegalStateException("bilinmeyen tarif türü: " + source);
            }
        }

        var result = new JsonObject();
        groups.forEach(result::add);
        EMPTY_GROUPS.forEach(name -> result.add(name, JsonNull.INSTANCE));

        var potionMixes = new JsonArray();
        for (var element : cloudburst.getAsJsonArray("potionMixes")) {
            var source = element.getAsJsonObject();
            var mix = new JsonObject();
            mix.add("input", itemWithData(source, "input"));
            mix.add("output", itemWithData(source, "output"));
            mix.add("reagent", itemWithData(source, "reagent"));
            potionMixes.add(mix);
        }
        result.add("potionMixes", potionMixes);

        var containerMixes = new JsonArray();
        for (var element : cloudburst.getAsJsonArray("containerMixes")) {
            var source = element.getAsJsonObject();
            var mix = new JsonObject();
            mix.addProperty("input", source.get("inputId").getAsString());
            mix.addProperty("output", source.get("outputId").getAsString());
            mix.addProperty("reagent", source.get("reagentId").getAsString());
            containerMixes.add(mix);
        }
        result.add("containerMixes", containerMixes);
        return result;
    }

    private static JsonObject shapeless(JsonObject source) throws IOException {
        var recipe = common(source);
        var inputs = new JsonArray();
        for (var input : source.getAsJsonArray("input")) {
            inputs.add(descriptor(input.getAsJsonObject()));
        }
        recipe.add("input", inputs);
        recipe.add("output", outputs(source.get("output")));
        return recipe;
    }

    private static JsonObject shaped(JsonObject source) throws IOException {
        var recipe = common(source);
        var inputs = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.getAsJsonObject("input").entrySet()) {
            inputs.add(entry.getKey(), descriptor(entry.getValue().getAsJsonObject()));
        }
        var pattern = source.getAsJsonArray("shape");
        var width = 0;
        for (var row : pattern) {
            width = Math.max(width, row.getAsString().length());
        }
        recipe.add("input", inputs);
        recipe.add("output", outputs(source.get("output")));
        recipe.add("pattern", pattern.deepCopy());
        recipe.addProperty("height", pattern.size());
        recipe.addProperty("width", width);
        return recipe;
    }

    private static JsonObject smithingTransform(JsonObject source) throws IOException {
        var input = source.getAsJsonObject("input");
        var output = outputs(source.get("output"));
        var recipe = new JsonObject();
        // CloudburstMC bu türde kimlik yazmıyor; Allay'in 12 dönüşüm tarifinin hepsi "smithing_<çıktı>" adını taşıyor.
        var outputId = output.get(0).getAsJsonObject().get("item").getAsString();
        recipe.addProperty("id", "minecraft:smithing_" + outputId.substring(outputId.indexOf(':') + 1));
        recipe.add("template", descriptor(input.getAsJsonObject("template")));
        recipe.add("base", descriptor(input.getAsJsonObject("base")));
        recipe.add("addition", descriptor(input.getAsJsonObject("addition")));
        recipe.add("output", output);
        recipe.addProperty("priority", 0);
        recipe.addProperty("tag", source.get("block").getAsString());
        return recipe;
    }

    private static JsonObject smithingTrim(JsonObject source) {
        var input = source.getAsJsonObject("input");
        var recipe = new JsonObject();
        recipe.addProperty("id", TRIM_RECIPE_ID);
        recipe.add("template", descriptor(input.getAsJsonObject("template")));
        recipe.add("base", descriptor(input.getAsJsonObject("base")));
        recipe.add("addition", descriptor(input.getAsJsonObject("addition")));
        recipe.addProperty("priority", 0);
        recipe.addProperty("tag", source.get("block").getAsString());
        return recipe;
    }

    private static JsonObject common(JsonObject source) {
        var recipe = new JsonObject();
        var tag = source.get("block").getAsString();
        recipe.addProperty("id", source.get("id").getAsString());
        var priority = source.has("priority") ? source.get("priority").getAsInt() : 0;
        if (COOKING_TAGS.contains(tag)) {
            priority = hasTagInput(source) ? -1 : 0;
        }
        recipe.addProperty("priority", priority);
        recipe.addProperty("tag", tag);
        return recipe;
    }

    private static boolean hasTagInput(JsonObject source) {
        for (var input : source.getAsJsonArray("input")) {
            if (input.getAsJsonObject().get("type").getAsString().equals("item_tag")) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject descriptor(JsonObject source) {
        var result = new JsonObject();
        result.addProperty("count", source.get("count").getAsInt());
        switch (source.get("type").getAsString()) {
            case "default" -> {
                result.addProperty("item", source.get("id").getAsString());
                var aux = source.get("auxValue").getAsInt();
                if (aux != ANY_AUX && aux != 0) {
                    result.addProperty("data", aux);
                }
            }
            case "item_tag" -> result.addProperty("tag", source.get("itemTag").getAsString());
            default -> throw new IllegalStateException("bilinmeyen girdi tanımlayıcısı: " + source);
        }
        return result;
    }

    private static JsonArray outputs(JsonElement element) throws IOException {
        var result = new JsonArray();
        var sources = element.isJsonArray() ? element.getAsJsonArray() : singleton(element);
        for (var output : sources) {
            var source = output.getAsJsonObject();
            var item = new JsonObject();
            item.addProperty("count", source.has("count") ? source.get("count").getAsInt() : 1);
            item.addProperty("item", source.get("id").getAsString());
            if (source.has("damage")) {
                item.addProperty("data", source.get("damage").getAsInt());
            }
            if (source.has("nbt_b64")) {
                item.addProperty("nbt", bigEndianBase64(source.get("nbt_b64").getAsString()));
            }
            result.add(item);
        }
        return result;
    }

    private static JsonObject itemWithData(JsonObject source, String prefix) {
        var item = new JsonObject();
        item.addProperty("data", source.get(prefix + "Meta").getAsInt());
        item.addProperty("item", source.get(prefix + "Id").getAsString());
        return item;
    }

    private static JsonArray singleton(JsonElement element) {
        var array = new JsonArray();
        array.add(element);
        return array;
    }

    /** CloudburstMC küçük uçlu, Allay ({@code AllayNBTUtils.base64ToNbt}) büyük uçlu NBT bekler. */
    private static String bigEndianBase64(String littleEndianBase64) throws IOException {
        var tag = DataFiles.readLittleEndianBase64(littleEndianBase64);
        var bytes = new ByteArrayOutputStream();
        try (var writer = NbtUtils.createWriter(bytes)) {
            writer.writeTag(tag);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
