package org.allaymc.data.importer;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kaynak dosyaları commit'e sabitlenmiş GitHub adreslerinden indirir ve önbellekte tutar.
 *
 * <p>Adresler commit özetine bağlı olduğu için içerik değişmez. İndirilen her dosyanın SHA-1'i kaydedilir ve
 * üretilen README'ye yazılır; böylece bir sonraki güncellemede kaynağın gerçekten aynı olup olmadığı görülür.</p>
 */
@Slf4j
final class SourceFetcher {

    private final Path cacheDir;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final Map<String, String> sha1ByUrl = new TreeMap<>();

    SourceFetcher(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * @param repository {@code sahip/depo}
     * @param commit     tam commit özeti
     * @param path       depo içindeki yol
     */
    Path fetch(String repository, String commit, String path) throws IOException, InterruptedException {
        var url = "https://raw.githubusercontent.com/" + repository + "/" + commit + "/" + path;
        var target = cacheDir.resolve(repository).resolve(commit).resolve(path);
        if (!Files.isRegularFile(target)) {
            Files.createDirectories(target.getParent());
            log.info("İndiriliyor: {}", url);
            var response = client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + url);
            }
            var temporary = target.resolveSibling(target.getFileName() + ".part");
            Files.write(temporary, response.body());
            Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        sha1ByUrl.put(repository + "@" + commit.substring(0, 8) + ":" + path, sha1(target));
        return target;
    }

    Map<String, String> sha1ByUrl() {
        return sha1ByUrl;
    }

    private static String sha1(Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
