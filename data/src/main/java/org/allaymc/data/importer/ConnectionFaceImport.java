package org.allaymc.data.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.utils.hash.HashUtils;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.allaymc.data.importer.DataFiles.readGzipNbt;
import static org.allaymc.data.importer.DataFiles.readJson;

/**
 * Vanilla kahinin bağlantı yüzü ölçümünden ({@code unpacked/bds_connection_faces.json}) çalışma zamanı tablosunu
 * ({@code block_connection_faces.json}) üretir (yol haritası Adım 6.8a).
 *
 * <p>Tablo, her blok durumunun hangi yatay yüzüne çitin ve cam panelin/parmaklığın bağlandığını söyler. Bu bilgi Allay'in
 * şekil verisinden türetilemiyor (ruh kumu, çamur ve bal bloğu bağlanır; kaktüs, yaprak ve basamak bağlanmaz), bu yüzden
 * BDS'te her durumun dört yanına sonda konup ölçüldü: {@code tools/vanilla-oracle/run_oracle.py --mode faces}.</p>
 *
 * <p>Türetme kuralları:</p>
 * <ul>
 *     <li><b>Aile ölçümü atılır.</b> Sonda kendi ailesine yüzden bağımsız bağlanır (nether tuğlası çiti nether tuğlası
 *     çitine ve çit kapısına, parmaklık panele/parmaklığa/duvara/{@code border_block}'a). Bu bağlantıları kural modülü
 *     kendisi verir; ölçüm yüzü ayırt edemediği için o sondanın o bloktaki sonucu tabloya girmez.</li>
 *     <li><b>İki sonda aynı sonucu vermeli.</b> Aile dışında 26.50'de çit ve panel aynı yüzlere bağlanıyor; tablo tek
 *     maske tutar. Farklı sonuç çıkarsa üretim durur (kural modülüne ikinci maske gerekir).</li>
 *     <li><b>Ölçüm sırasında değişen blok.</b> BDS bazı durumları kendiliğinden başka duruma çeviriyor (duvar bağlantısı,
 *     dolu raf, ölen mercan, desteksiz meşale). Sondanın okuduğu yüz son duruma aittir ve ona yazılır. Özgün duruma da
 *     yalnızca Allay'deki çarpışma şekli son durumla aynı ve boş değilse aktarılır; şekil farklıysa (duvar, çit) ya da
 *     blok çarpışmasızsa (havaya dönen düğme) özgün durum ölçülmemiş sayılır.</li>
 *     <li><b>Köşeli merdiven</b> tek başına kararlı değil, komşu merdivenle birlikte ölçülür; komşunun durduğu yan
 *     ({@code -}) ölçülmez. O yana bir çit ancak komşu merdiven kalkınca gelebilir, o anda merdivenin köşesi de değişir.</li>
 *     <li>Ölçülmemiş yan bağlanmaz sayılır. Bu durumların hepsi desteksiz duramayan ince bloklar (düğme, meşale, ray,
 *     çerçeve...) ya da şekli değişen bağlantılı bloklar; ölçülebilen bütün kardeş durumları da bağlanmıyor.</li>
 * </ul>
 *
 * <p>Çıktı biçimi: {@code {"<maske>": [hash, ...]}}; maske bitleri 0 kuzey, 1 doğu, 2 güney, 3 batı. Maskesi 0 olan durum
 * yazılmaz. {@code minecraft:unknown} Allay'deki özel hash'iyle (-2) yazılır.</p>
 *
 * <p>Çalıştırma (depo kökünden): {@code ./gradlew :data:runMain -PmainClass=org.allaymc.data.importer.ConnectionFaceImport}.
 * Doğrulama: {@code ConnectionFaceImportTest}.</p>
 */
@Slf4j
public final class ConnectionFaceImport {

    static final Path RAW = Path.of("data/resources/unpacked/bds_connection_faces.json");
    static final Path PALETTE = Path.of("data/resources/unpacked/block_palette.nbt");
    static final Path BLOCK_STATES = Path.of("data/resources/block_states.json");
    static final Path OUTPUT = Path.of("data/resources/block_connection_faces.json");

    private static final String FENCE_PROBE = "minecraft:nether_brick_fence";
    private static final String PANE_PROBE = "minecraft:iron_bars";
    private static final List<String> SIDES = List.of("north", "east", "south", "west");
    private static final int UNKNOWN_HASH = -2;
    private static final String EMPTY_SHAPE = "[]";

    private ConnectionFaceImport() {
    }

    public static void main(String[] args) {
        try {
            var result = derive(readJson(RAW).getAsJsonObject(), readPalette(), readJson(BLOCK_STATES).getAsJsonArray());
            DataFiles.writeJson(OUTPUT, result.table());
            log.info("Bağlantı yüzü tablosu yazıldı: {} ({} durum bağlanır; hiçbir durumu ölçülemeyen türler: {})",
                    OUTPUT.toAbsolutePath(), result.connecting(), result.unmeasuredTypes());
        } catch (Throwable t) {
            log.error("Bağlantı yüzü tablosu üretilemedi", t);
            System.exit(1);
        }
    }

    static List<NbtMap> readPalette() throws IOException {
        return readGzipNbt(PALETTE).getList("blocks", NbtType.COMPOUND);
    }

    /**
     * @param connecting      en az bir yanı bağlanan durum sayısı
     * @param unmeasuredTypes hiçbir durumunun hiçbir yanı ölçülemeyen blok türleri
     */
    record Result(JsonObject table, int connecting, SortedSet<String> unmeasuredTypes) {
    }

    static Result derive(JsonObject raw, List<NbtMap> palette, JsonArray blockStates) throws IOException {
        var probes = new ArrayList<String>();
        raw.getAsJsonArray("probes").forEach(probe -> probes.add(probe.getAsString()));
        if (!probes.equals(List.of(FENCE_PROBE, PANE_PROBE))) {
            throw new IllegalStateException("beklenmeyen sonda listesi: " + probes);
        }
        var sides = new ArrayList<String>();
        raw.getAsJsonArray("sides").forEach(side -> sides.add(side.getAsString()));
        if (!sides.equals(SIDES)) {
            throw new IllegalStateException("beklenmeyen yan sırası: " + sides);
        }

        var rows = raw.getAsJsonArray("states");
        if (rows.size() != palette.size()) {
            throw new IllegalStateException("ham tablo " + rows.size() + " durum, palet " + palette.size());
        }
        var names = new String[palette.size()];
        var hashes = new int[palette.size()];
        for (var i = 0; i < palette.size(); i++) {
            var state = palette.get(i);
            names[i] = state.getString("name");
            var rowName = rows.get(i).getAsJsonArray().get(0).getAsString();
            if (!rowName.equals(names[i])) {
                throw new IllegalStateException("ham tablo palet sırasını izlemiyor: " + i + " " + rowName + " != " + names[i]);
            }
            hashes[i] = names[i].equals("minecraft:unknown") ? UNKNOWN_HASH : HashUtils.fnv1a_32_nbt(NbtMap.builder()
                    .putString("name", names[i])
                    .putCompound("states", NbtMap.fromMap(new TreeMap<>(state.getCompound("states"))))
                    .build());
        }

        var shapeByHash = new HashMap<Integer, String>();
        blockStates.forEach(element -> {
            var data = element.getAsJsonObject();
            shapeByHash.put((int) data.get("blockStateHash").getAsLong(), data.get("collisionShape").toString());
        });

        // faces[durum][yan]: null ölçülmedi
        var faces = new Boolean[palette.size()][SIDES.size()];
        var sources = new String[palette.size()][SIDES.size()];
        for (var state = 0; state < palette.size(); state++) {
            var row = rows.get(state).getAsJsonArray();
            for (var probe = 0; probe < probes.size(); probe++) {
                var cell = row.get(1 + probe);
                String measured;
                List<Integer> targets;
                if (cell.isJsonPrimitive()) {
                    measured = cell.getAsString();
                    targets = List.of(state);
                } else {
                    measured = cell.getAsJsonArray().get(0).getAsString();
                    var finalElement = cell.getAsJsonArray().get(1);
                    if (finalElement.isJsonNull()) {
                        continue;
                    }
                    var finalState = finalElement.getAsInt();
                    targets = new ArrayList<>(List.of(finalState));
                    var originalShape = shapeByHash.get(hashes[state]);
                    // Çarpışması olmayan blok (hava, su, meşale) yüzü taşımaz: havaya dönen düğmenin "bağlanmaz" sonucu
                    // düğmeye değil havaya aittir.
                    if (finalState != state && originalShape != null && !originalShape.equals(EMPTY_SHAPE)
                        && originalShape.equals(shapeByHash.get(hashes[finalState]))) {
                        targets.add(state);
                    }
                }
                for (var target : targets) {
                    if (isFamily(probes.get(probe), names[target])) {
                        continue;
                    }
                    for (var side = 0; side < SIDES.size(); side++) {
                        var symbol = measured.charAt(side);
                        if (symbol == '-') {
                            continue;
                        }
                        var value = symbol == '1';
                        var source = probes.get(probe) + " @" + state;
                        if (faces[target][side] != null && faces[target][side] != value) {
                            throw new IllegalStateException("çelişen ölçüm: " + names[target] + " (" + target + ") "
                                    + SIDES.get(side) + " yanı " + sources[target][side] + " → " + faces[target][side]
                                    + ", " + source + " → " + value);
                        }
                        faces[target][side] = value;
                        sources[target][side] = source;
                    }
                }
            }
        }

        var hashesByMask = new TreeMap<Integer, TreeSet<Integer>>();
        var measuredTypes = new HashSet<String>();
        var connecting = 0;
        for (var state = 0; state < palette.size(); state++) {
            var mask = 0;
            var known = false;
            for (var side = 0; side < SIDES.size(); side++) {
                if (faces[state][side] != null) {
                    known = true;
                    if (faces[state][side]) {
                        mask |= 1 << side;
                    }
                }
            }
            if (known) {
                measuredTypes.add(names[state]);
            }
            if (mask != 0) {
                hashesByMask.computeIfAbsent(mask, key -> new TreeSet<>()).add(hashes[state]);
                connecting++;
            }
        }

        var table = new JsonObject();
        hashesByMask.forEach((mask, maskHashes) -> {
            var array = new JsonArray();
            maskHashes.forEach(array::add);
            table.add(String.valueOf(mask), array);
        });
        var unmeasuredTypes = new TreeSet<>(Arrays.asList(names));
        unmeasuredTypes.removeAll(measuredTypes);
        return new Result(table, connecting, unmeasuredTypes);
    }

    /** Sondanın yüzden bağımsız bağlandığı bloklar; kural modülündeki aile kurallarının ad karşılığı. */
    static boolean isFamily(String probe, String name) {
        return switch (probe) {
            case FENCE_PROBE -> name.equals(FENCE_PROBE) || name.endsWith("fence_gate");
            case PANE_PROBE -> name.endsWith("_pane") || name.endsWith("_bars") || name.endsWith("_wall")
                               || name.equals("minecraft:border_block");
            default -> throw new IllegalArgumentException(probe);
        };
    }
}
