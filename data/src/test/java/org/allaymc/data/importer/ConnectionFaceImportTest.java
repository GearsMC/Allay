package org.allaymc.data.importer;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.allaymc.data.importer.DataFiles.readJson;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bağlantı yüzü tablosunun ({@code block_connection_faces.json}) ham kahin ölçümüyle tutarlı olduğunu doğrular.
 *
 * <p>Tablonun kendisi elle yazılmaz; bu test {@link ConnectionFaceImport} türetmesini yeniden çalıştırıp dosyayla
 * karşılaştırır. Ölçüm ya da palet değişip tablo yeniden üretilmezse burada düşer.</p>
 */
class ConnectionFaceImportTest {

    private static ConnectionFaceImport.Result result;

    @BeforeAll
    static void derive() throws IOException {
        result = ConnectionFaceImport.derive(
                readJson(ConnectionFaceImport.RAW).getAsJsonObject(),
                ConnectionFaceImport.readPalette(),
                readJson(ConnectionFaceImport.BLOCK_STATES).getAsJsonArray());
    }

    @Test
    void committedTableMatchesDerivation() throws IOException {
        assertEquals(DataFiles.sorted(result.table()), readJson(ConnectionFaceImport.OUTPUT),
                "block_connection_faces.json güncel değil: ./gradlew :data:runMain -PmainClass=org.allaymc.data.importer.ConnectionFaceImport");
    }

    @Test
    void everyHashIsAKnownBlockState() throws IOException {
        var known = new HashSet<Integer>();
        readJson(ConnectionFaceImport.BLOCK_STATES).getAsJsonArray()
                .forEach(element -> known.add((int) element.getAsJsonObject().get("blockStateHash").getAsLong()));
        var table = readJson(ConnectionFaceImport.OUTPUT).getAsJsonObject();
        for (var entry : table.entrySet()) {
            var mask = Integer.parseInt(entry.getKey());
            assertTrue(mask > 0 && mask < 16, "geçersiz maske " + mask);
            entry.getValue().getAsJsonArray().forEach(hash ->
                    assertTrue(known.contains(hash.getAsInt()), "block_states.json'da olmayan durum: " + hash));
        }
    }

    /**
     * Hiçbir durumu ölçülemeyen türler: ölçüm sırasında havaya ya da suya dönen, çarpışması olmayan bloklar. Liste
     * büyürse yeni bir tür ölçümden kaçıyor demektir; ona bakılmadan tabloya "bağlanmaz" olarak girmemeli.
     */
    @Test
    void onlyNonCollidingBlocksStayUnmeasured() {
        assertEquals(new TreeSet<>(Set.of(
                "minecraft:brain_coral_wall_fan", "minecraft:bubble_coral_wall_fan", "minecraft:fire_coral_wall_fan",
                "minecraft:horn_coral_wall_fan", "minecraft:tube_coral_wall_fan", "minecraft:bubble_column",
                "minecraft:chalkboard", "minecraft:flowing_lava", "minecraft:flowing_water", "minecraft:moving_block",
                "minecraft:soul_fire", "minecraft:tripwire_hook", "minecraft:unlit_redstone_torch"
        )), result.unmeasuredTypes());
    }

    @Test
    void familyMembersAreLeftToTheRuleModule() {
        assertTrue(ConnectionFaceImport.isFamily("minecraft:nether_brick_fence", "minecraft:nether_brick_fence"));
        assertTrue(ConnectionFaceImport.isFamily("minecraft:nether_brick_fence", "minecraft:crimson_fence_gate"));
        assertFalse(ConnectionFaceImport.isFamily("minecraft:nether_brick_fence", "minecraft:oak_fence"));
        assertTrue(ConnectionFaceImport.isFamily("minecraft:iron_bars", "minecraft:waxed_copper_bars"));
        assertTrue(ConnectionFaceImport.isFamily("minecraft:iron_bars", "minecraft:hard_glass_pane"));
        assertTrue(ConnectionFaceImport.isFamily("minecraft:iron_bars", "minecraft:border_block"));
        assertFalse(ConnectionFaceImport.isFamily("minecraft:iron_bars", "minecraft:brain_coral_wall_fan"));
    }

    @SuppressWarnings("unused")
    private static JsonObject table() {
        return result.table();
    }
}
