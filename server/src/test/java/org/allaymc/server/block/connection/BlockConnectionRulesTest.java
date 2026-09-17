package org.allaymc.server.block.connection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.property.enums.MinecraftCorner;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.joml.Vector3i;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BlockConnectionRules}'u vanilla kahinin altın tablosuyla sınar.
 *
 * <p>Her senaryo bellekteki bir ızgaraya kurulur: yerleşimler {@code scenarios.json}'dan, okunan hücreler BDS'in son
 * halinden (ör. desteksiz kanca havaya dönmüş) alınır. Okunan her bağlantı/köşe bloğunun bağlantı durumları sıfırlanıp
 * kurala yeniden hesaplatılır ve BDS'in hesapladığıyla karşılaştırılır. Böylece test kuralın kendisini sınar; altın
 * durumdan başlayıp "değişmedi" demek yetmez.</p>
 */
@ExtendWith(AllayTestExtension.class)
class BlockConnectionRulesTest {

    private static final String GOLDEN = "vanilla-oracle/1.26.51.1.json";
    // Test çalışma dizini .test/; senaryo kaynağı araç klasöründe.
    private static final Path SCENARIOS = Path.of("..", "tools", "vanilla-oracle", "scenarios.json");

    private static JsonObject golden;
    private static JsonArray scenarios;

    @BeforeAll
    static void load() throws IOException, NoSuchAlgorithmException {
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                BlockConnectionRulesTest.class.getClassLoader().getResourceAsStream(GOLDEN)), StandardCharsets.UTF_8)) {
            golden = JsonParser.parseReader(reader).getAsJsonObject();
        }
        var bytes = Files.readAllBytes(SCENARIOS);
        var sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        assertEquals(golden.get("scenarioSha1").getAsString(), sha1,
                "scenarios.json altın tablodan sonra değişmiş; kahini yeniden çalıştır");
        scenarios = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("scenarios");
    }

    @Test
    void rulesReproduceVanillaScenarios() {
        var results = golden.getAsJsonObject("results");
        var failures = new ArrayList<String>();
        var checked = 0;
        for (var element : scenarios) {
            var scenario = element.getAsJsonObject();
            var id = scenario.get("id").getAsString();
            var grid = new HashMap<Vector3i, BlockState>();
            place(grid, scenario.getAsJsonArray("place"));
            if (scenario.has("then")) {
                place(grid, scenario.getAsJsonArray("then"));
            }
            var cells = results.getAsJsonObject(id).getAsJsonObject("cells");
            for (var cell : cells.entrySet()) {
                grid.put(position(cell.getKey()), state(cell.getValue().getAsJsonObject()));
            }

            for (var cell : cells.entrySet()) {
                var pos = position(cell.getKey());
                var expected = grid.get(pos);
                if (!BlockConnectionRules.isConnectionBlock(expected)) {
                    continue;
                }
                var actual = BlockConnectionRules.update(reset(expected), face -> grid.getOrDefault(
                        new Vector3i(pos).add(face.getOffset()), BlockTypes.AIR.getDefaultState()));
                checked++;
                if (!actual.equals(expected)) {
                    failures.add(id + " " + cell.getKey() + ": beklenen " + expected.getPropertyValues().values()
                                 + ", hesaplanan " + actual.getPropertyValues().values());
                }
            }
        }
        assertTrue(checked > 450, "yalnızca " + checked + " hücre sınandı");
        assertTrue(failures.isEmpty(), failures.size() + "/" + checked + " hücre vanilladan farklı:\n" + String.join("\n", failures));
    }

    @Test
    void stairsCornerSideFacesDifferFromJava() {
        // Ölçüm: iç köşeli merdivende yalnızca arka yüz bağlanıyor. Java'da köşenin yan yüzü de dolu sayılırdı.
        var stairs = BlockTypes.OAK_STAIRS.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.WEIRDO_DIRECTION, 0)
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER, MinecraftCorner.INNER_LEFT);
        assertTrue(BlockConnectionFaces.isConnectable(stairs, BlockFace.EAST));
        assertFalse(BlockConnectionFaces.isConnectable(stairs, BlockFace.NORTH));
        assertFalse(BlockConnectionFaces.isConnectable(stairs, BlockFace.SOUTH));
    }

    private static BlockState reset(BlockState state) {
        if (state.getBlockType().hasProperty(BlockPropertyTypes.MINECRAFT_CORNER)) {
            return state.setPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER, MinecraftCorner.NONE);
        }
        for (var property : List.of(BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH, BlockPropertyTypes.MINECRAFT_CONNECTION_EAST,
                BlockPropertyTypes.MINECRAFT_CONNECTION_SOUTH, BlockPropertyTypes.MINECRAFT_CONNECTION_WEST)) {
            state = state.setPropertyValue(property, false);
        }
        return state;
    }

    private static void place(Map<Vector3i, BlockState> grid, JsonArray placements) {
        for (var element : placements) {
            var placement = element.getAsJsonObject();
            var at = placement.getAsJsonArray("at");
            grid.put(new Vector3i(at.get(0).getAsInt(), at.get(1).getAsInt(), at.get(2).getAsInt()), state(placement));
        }
    }

    private static Vector3i position(String key) {
        var parts = key.split(",");
        return new Vector3i(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    /** Palet adı + (kısmi) durumlar; Script API'nin palette olmayan eski durum adları ({@code wood_type}) atlanır. */
    private static BlockState state(JsonObject block) {
        var name = block.get("name").getAsString();
        var type = Registries.BLOCKS.get(new Identifier(name));
        assertNotNull(type, "Allay'de olmayan blok: " + name);
        var state = type.getDefaultState();
        if (block.has("states")) {
            for (Map.Entry<String, JsonElement> entry : block.getAsJsonObject("states").entrySet()) {
                var property = type.getProperties().get(entry.getKey());
                if (property == null) {
                    continue;
                }
                var json = entry.getValue().getAsJsonPrimitive();
                Object value = json.isBoolean() ? json.getAsBoolean() : json.isNumber() ? json.getAsInt() : json.getAsString();
                state = state.setPropertyValue(property.tryCreateValue(value));
            }
        }
        return state;
    }
}
