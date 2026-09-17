package org.allaymc.data.importer;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Collectors;

import static org.allaymc.data.importer.DataFiles.*;

/**
 * Bedrock veri setini Endstone dökümü yerine herkese açık kaynaklardan üretir (yol haritası Adım 5.2).
 *
 * <p>Endstone DevTools yalnızca Windows'ta çalışıyor. Bu araç aynı dosyaları CloudburstMC/Data, Altay
 * (PocketMine'ın devamı), Mojang {@code bedrock-samples} ve vanilla kahinin BDS kayıt dökümünden
 * ({@code tools/vanilla-oracle}, {@code --mode dump}) üretir. Kaynaklar commit özetine sabitlidir.</p>
 *
 * <p>Çıktı {@link #OUTPUT} altına yazılır ve sunucu tarafından okunmaz. {@code resources/} ile {@code unpacked/}
 * alt klasörleri {@code data/resources} ve {@code data/resources/unpacked} düzenini aynen izler; veri devreye
 * alınırken dosyalar yerine taşınır (26.50 için Adım 6). Doğrulama: {@code BedrockDataTest}.</p>
 *
 * <p>Çalıştırma (depo kökünden, ağ gerekir): {@code ./gradlew :data:importBedrockData}</p>
 */
@Slf4j
public final class BedrockDataImporter {

    static final Path OUTPUT = Path.of("data/resources/unpacked/staging-1.26.50");

    private static final String CLOUDBURST = "CloudburstMC/Data";
    private static final String CLOUDBURST_COMMIT = "3255e82c0f89496abb2fb9747f32f1bc2926bea6";
    private static final String ALTAY = "altayofficial/BedrockData";
    private static final String ALTAY_COMMIT = "190703c701dbb7fcd9ac38f5978af456a6365718";
    private static final String MOJANG = "Mojang/bedrock-samples";
    private static final String MOJANG_COMMIT = "46ba6ea985fb5a92d79a9419198f10dda14c199d";

    private BedrockDataImporter() {
    }

    public static void main(String[] args) {
        try {
            run();
        } catch (Throwable t) {
            // Allay'in log yapılandırması standart hata akışını yutuyor; hata logdan görünsün.
            log.error("İçe aktarma başarısız", t);
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        var fetcher = new SourceFetcher(Path.of(System.getProperty("user.home"), ".cache", "gears-bedrock-data"));
        var resources = Path.of("data/resources");
        var unpacked = resources.resolve("unpacked");
        var outResources = OUTPUT.resolve("resources");
        var outUnpacked = OUTPUT.resolve("unpacked");
        var dump = readJson(OUTPUT.resolve("bds_registry_dump.json")).getAsJsonObject();

        // Bloklar
        var palette = BlockDataImport.palette(readGzipNbt(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "block_palette.nbt")));
        writeGzipNbt(outUnpacked.resolve("block_palette.nbt"), palette);
        writeJson(outUnpacked.resolve("block_states_raw.json"), readJson(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "blocks.json")));
        var blockTypes = BlockDataImport.blockTypes(
                palette, dump.getAsJsonObject("blocks"), readJson(resources.resolve("block_types.json")).getAsJsonObject());
        writeJson(outResources.resolve("block_types.json"), blockTypes);
        writeJson(outUnpacked.resolve("block_tags.json"), BlockDataImport.aggregateTags(blockTypes));

        // Eşyalar
        var runtimeStates = readJson(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "runtime_item_states.json")).getAsJsonArray();
        var items = ItemDataImport.itemsRaw(runtimeStates, readJson(unpacked.resolve("items_raw.json")).getAsJsonObject(), dump);
        writeJson(outUnpacked.resolve("items_raw.json"), items);
        writeJson(outUnpacked.resolve("item_tags.json"), BlockDataImport.aggregateTags(items));
        writeGzipNbt(outResources.resolve("item_components.nbt"), ItemDataImport.itemComponents(
                readGzipNbt(resources.resolve("item_components.nbt")),
                readJson(unpacked.resolve("items_raw.json")).getAsJsonObject(),
                readGzipNbt(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "item_components.nbt")),
                runtimeStates));
        var creative = ItemDataImport.creative(
                readJson(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "creative_items.json")).getAsJsonObject());
        writeJson(outResources.resolve("creative_groups.json"), creative.groups());
        writeGzipNbt(outResources.resolve("creative_items.nbt"), creative.items());

        // Tarifler
        writeJson(outResources.resolve("recipes.json"), RecipeImport.recipes(
                readJson(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "recipes.json")).getAsJsonObject(),
                readJson(resources.resolve("recipes.json")).getAsJsonObject()));

        // Olduğu gibi kopyalananlar
        copy(fetcher.fetch(CLOUDBURST, CLOUDBURST_COMMIT, "stripped_biome_definitions.json"), outResources.resolve("biome_definitions.json"));
        copy(fetcher.fetch(ALTAY, ALTAY_COMMIT, "entity_id_map.json"), outUnpacked.resolve("entity_id_map.json"));
        for (var file : new String[]{"mojang-blocks.json", "mojang-items.json"}) {
            copy(fetcher.fetch(MOJANG, MOJANG_COMMIT, "metadata/vanilladata_modules/" + file), outUnpacked.resolve(file));
        }
        for (var file : new String[]{"music_definitions.json", "sound_definitions.json"}) {
            copy(fetcher.fetch(MOJANG, MOJANG_COMMIT, "resource_pack/sounds/" + file), outUnpacked.resolve(file));
        }
        try (var languages = Files.list(unpacked.resolve("lang_raw/vanilla"))) {
            for (var language : languages.map(path -> path.getFileName().toString()).sorted().toList()) {
                copy(fetcher.fetch(MOJANG, MOJANG_COMMIT, "resource_pack/texts/" + language),
                        outUnpacked.resolve("lang_raw/vanilla").resolve(language));
            }
        }

        writeSources(fetcher.sha1ByUrl());
        log.info("26.50 veri seti yazıldı: {}", OUTPUT.toAbsolutePath());
    }

    private static void copy(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeSources(Map<String, String> sha1ByUrl) throws IOException {
        var rows = sha1ByUrl.entrySet().stream()
                .map(entry -> "| `" + entry.getKey() + "` | `" + entry.getValue() + "` |")
                .collect(Collectors.joining("\n"));
        Files.writeString(OUTPUT.resolve("SOURCES.md"), """
                # 26.50 veri seti kaynakları

                Bu dosya `BedrockDataImporter` tarafından üretilir, elle düzenlenmez. Kaynaklar commit özetine sabitlidir;
                SHA-1 değerleri bir sonraki çalıştırmada kaynağın değişip değişmediğini görmek içindir.

                BDS kayıt dökümü (`bds_registry_dump.json`) ayrı üretilir: `tools/vanilla-oracle/run_oracle.py --mode dump`.

                | Kaynak | SHA-1 |
                |---|---|
                """ + rows + "\n");
    }
}
