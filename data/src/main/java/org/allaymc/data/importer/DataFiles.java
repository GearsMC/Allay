package org.allaymc.data.importer;

import com.google.gson.*;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.TreeMap;

/**
 * İçe aktarma aracının dosya okuma/yazma yardımcıları.
 *
 * <p>Endstone dökümüyle aynı biçim korunur: JSON tek satır ve anahtarları alfabetik, NBT gzip'li büyük uçlu.
 * Böylece Adım 6'da dosyalar yerine konduğunda git farkı yalnızca gerçek veri değişikliğini gösterir.</p>
 */
@UtilityClass
final class DataFiles {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    static JsonElement readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }

    static void writeJson(Path path, JsonElement element) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(sorted(element)));
    }

    /** Nesne anahtarlarını her düzeyde alfabetik sıralar; dizi sırası korunur. */
    static JsonElement sorted(JsonElement element) {
        if (element.isJsonObject()) {
            var entries = new TreeMap<String, JsonElement>();
            element.getAsJsonObject().entrySet().forEach(entry -> entries.put(entry.getKey(), sorted(entry.getValue())));
            var result = new JsonObject();
            entries.forEach(result::add);
            return result;
        }
        if (element.isJsonArray()) {
            var result = new JsonArray();
            element.getAsJsonArray().forEach(child -> result.add(sorted(child)));
            return result;
        }
        return element;
    }

    static NbtMap readGzipNbt(Path path) throws IOException {
        try (var reader = NbtUtils.createGZIPReader(new BufferedInputStream(Files.newInputStream(path)))) {
            return (NbtMap) reader.readTag();
        }
    }

    static void writeGzipNbt(Path path, NbtMap tag) throws IOException {
        Files.createDirectories(path.getParent());
        try (var writer = NbtUtils.createGZIPWriter(new BufferedOutputStream(Files.newOutputStream(path)))) {
            writer.writeTag(tag);
        }
    }

    /** CloudburstMC/Data {@code *_b64} alanları küçük uçlu NBT'dir. */
    static NbtMap readLittleEndianBase64(String base64) throws IOException {
        try (var reader = NbtUtils.createReaderLE(new ByteArrayInputStream(Base64.getDecoder().decode(base64)))) {
            return (NbtMap) reader.readTag();
        }
    }
}
