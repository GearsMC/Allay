package org.allaymc.server.network.protocol;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.utils.hash.HashUtils;
import org.allaymc.server.datastruct.palette.Palette;
import org.allaymc.server.world.chunk.AllayChunkSection;
import org.allaymc.server.world.chunk.ChunkEncoder;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.joml.Vector3i;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sunucu blok durumlarının her protokolde o istemcinin resmi paletindeki bir kimliğe çevrildiğini doğrular.
 *
 * <p>Kahin sunucu kodu değil, {@code data/resources/protocol_palettes} altındaki resmi paletlerdir: her girdinin
 * hash'i burada bağımsız olarak hesaplanır. Bir durumun kimliği istemcinin paletinde yoksa istemci bloğu çizmez
 * (26.50'de merdiven, çit, cam panel, parmaklık ve tuzak ipi bu yüzden görünmez olmuştu).</p>
 */
@ExtendWith(AllayTestExtension.class)
class BlockNetworkIdMappingTest {
    /** İstemcinin {@code minecraft:unknown} için kullandığı özel kimlik. */
    private static final int UNKNOWN_BLOCK_ID = -2;

    /** Protokol numarası → o sürümün resmi palet dosyası. Yeni sürüm eklenince buraya da eklenir. */
    private static final Map<Integer, String> EXPECTED_PALETTES = Map.ofEntries(
            Map.entry(818, "1_21_90"),
            Map.entry(819, "1_21_90"),
            Map.entry(827, "1_21_100"),
            Map.entry(844, "1_21_111"),
            Map.entry(859, "1_21_111"),
            Map.entry(860, "1_21_111"),
            Map.entry(898, "1_21_111"),
            Map.entry(924, "1_21_111"),
            Map.entry(944, "1_26_10"),
            Map.entry(975, "1_26_20"),
            Map.entry(1001, "1_26_30"),
            Map.entry(2168, "1_26_40"),
            Map.entry(2169, "1_26_40"),
            Map.entry(2192, "1_26_50"),
            Map.entry(2193, "1_26_50")
    );

    private static final Map<String, OfficialPalette> PALETTES = new HashMap<>();
    private static ProtocolRegistry registry;
    private static List<BlockState> vanillaStates;

    @BeforeAll
    static void setUp() {
        registry = ProtocolRegistry.getDefault();
        vanillaStates = Registries.BLOCKS.getContent().values().stream()
                .filter(blockType -> isVanilla(blockType.getIdentifier()))
                .flatMap(blockType -> blockType.getAllStates().stream())
                .toList();
    }

    @Test
    void everyInternationalProtocolIsCoveredByTheExpectedPaletteTable() {
        var registered = new TreeSet<Integer>();
        for (var protocol : registry.getSupported(ClientVariant.INTERNATIONAL)) {
            registered.add(protocol.getProtocolVersion());
        }
        assertEquals(new TreeSet<>(EXPECTED_PALETTES.keySet()), registered);
    }

    @Test
    void everyVanillaStateMapsIntoTheOfficialPaletteOfItsProtocol() {
        for (var entry : EXPECTED_PALETTES.entrySet()) {
            var protocol = international(entry.getKey());
            var palette = palette(entry.getValue());
            var encoder = protocol.getEncoder();
            for (var state : vanillaStates) {
                int id = encoder.networkBlockId(state);
                assertTrue(
                        id == UNKNOWN_BLOCK_ID || palette.hashes().contains(id),
                        () -> protocol + ": " + state + " -> " + id + " resmi palette yok"
                );
            }
        }
    }

    @Test
    void protocolsWhosePaletteMatchesServerDataSendStateHashesUnchanged() {
        for (int version : List.of(1001, 2168, 2169)) {
            var encoder = international(version).getEncoder();
            for (var state : vanillaStates) {
                assertEquals(state.blockStateHash(), encoder.networkBlockId(state), () -> "v" + version + ": " + state);
            }
        }
    }

    @Test
    void v2193SendsTheDefaultVariantOfStatesReshapedIn2650() {
        var encoder = international(2193).getEncoder();
        var palette = palette("1_26_50");

        long changed = vanillaStates.stream()
                .filter(state -> encoder.networkBlockId(state) != state.blockStateHash())
                .count();
        assertEquals(584, changed, "26.50'de durum kümesi değişen 121 türün 584 durumu çevrilmeli");

        var fence = BlockTypes.OAK_FENCE.getDefaultState();
        assertEquals(palette.hashOf("minecraft:oak_fence", Map.of(
                "minecraft:connection_north", 0,
                "minecraft:connection_east", 0,
                "minecraft:connection_south", 0,
                "minecraft:connection_west", 0
        )), encoder.networkBlockId(fence));

        var stairs = BlockTypes.OAK_STAIRS.getDefaultState();
        var expectedStairs = new HashMap<String, Object>();
        stairs.getPropertyValues().forEach((type, value) -> expectedStairs.put(type.getName(), value.getSerializedValue()));
        expectedStairs.put("minecraft:corner", "none");
        assertEquals(palette.hashOf("minecraft:oak_stairs", expectedStairs), encoder.networkBlockId(stairs));
    }

    @Test
    void blockTypesUnknownToAnOldClientFallBackToTheUnknownBlock() {
        assertFalse(palette("1_21_90").names().contains("minecraft:cinnabar"));
        assertEquals(UNKNOWN_BLOCK_ID, international(818).getEncoder().networkBlockId(BlockTypes.CINNABAR.getDefaultState()));
    }

    /**
     * Resmi paletlerde yalnızca vanilla bloklar var. Eklentinin ya da testin kaydettiği başka ad alanındaki bir blok
     * (özel blok tanımı olsun olmasın) palete eşlenmeye çalışılırsa bilinmeyen bloğa düşer ve görünmez olur.
     */
    @Test
    void nonVanillaBlocksPassThroughUnchanged() {
        var nonVanilla = Registries.BLOCKS.getContent().values().stream()
                .filter(blockType -> !isVanilla(blockType.getIdentifier()))
                .flatMap(blockType -> blockType.getAllStates().stream())
                .toList();
        assertFalse(nonVanilla.isEmpty(), "test kaydında vanilla olmayan blok bekleniyordu");

        for (int version : List.of(818, 2193)) {
            var encoder = international(version).getEncoder();
            for (var state : nonVanilla) {
                assertEquals(state.blockStateHash(), encoder.networkBlockId(state), () -> "v" + version + ": " + state.getBlockType().getIdentifier());
            }
        }
    }

    @Test
    void blockUpdatesUseTheNetworkIdOfTheTargetProtocol() {
        var fence = BlockTypes.OAK_FENCE.getDefaultState();
        var position = new Vector3i(1, 64, 1);

        var v2193 = international(2193).getEncoder();
        assertNotEquals(fence.blockStateHash(), v2193.networkBlockId(fence));
        assertEquals(v2193.networkBlockId(fence), v2193.encodeBlockUpdate(position, 0, fence).getDefinition().runtimeId());

        var v1001 = international(1001).getEncoder();
        assertEquals(fence.blockStateHash(), v1001.encodeBlockUpdate(position, 0, fence).getDefinition().runtimeId());
    }

    @Test
    void creativeBlockItemsUseTheNetworkIdOfTheTargetProtocol() {
        var protocol = international(2193);
        int fenceId = protocol.getEncoder().networkBlockId(BlockTypes.OAK_FENCE.getDefaultState());

        var fenceItems = protocol.getData().creativeItems().stream()
                .map(creativeItem -> creativeItem.item())
                .filter(item -> item.getDefinition().identifier().equals("minecraft:oak_fence"))
                .toList();
        assertFalse(fenceItems.isEmpty(), "yaratıcı menüde meşe çiti bulunamadı");
        for (var item : fenceItems) {
            assertEquals(fenceId, item.getBlockDefinition().runtimeId());
        }
    }

    @Test
    void sectionBlobsUseTheNetworkIdOfTheTargetProtocol() {
        var fence = BlockTypes.OAK_FENCE.getDefaultState();
        var section = new AllayChunkSection((byte) 4);
        section.setBlockState(0, 0, 0, fence, 0);
        var encoder = international(2193).getEncoder();

        var buffer = Unpooled.wrappedBuffer(ChunkEncoder.encodeSectionBlob(section, encoder::networkBlockId));
        buffer.skipBytes(3); // sürüm, katman sayısı, bölüm Y
        var layer = new Palette<Integer>(0);
        layer.readFromNetwork(buffer, id -> id, null);

        assertEquals(encoder.networkBlockId(fence), layer.get(0));
    }

    private static boolean isVanilla(org.allaymc.api.utils.identifier.Identifier identifier) {
        return identifier.namespace().equals(org.allaymc.api.utils.identifier.Identifier.DEFAULT_NAMESPACE);
    }

    private static Protocol international(int version) {
        var protocol = registry.resolve(ClientVariant.INTERNATIONAL, version);
        assertNotNull(protocol, "v" + version + " kayıtlı değil");
        return protocol;
    }

    private static OfficialPalette palette(String name) {
        return PALETTES.computeIfAbsent(name, OfficialPalette::load);
    }

    /**
     * Resmi paletin yalnızca kimlik kümesi ve blok adları bellekte tutulur. Ayrıştırılmış 22 bin girdi sekiz palet için
     * statik alanda kalırsa test JVM'inin 512 MB sınırı aşılıyor ve sonraki test sınıfları düşüyor; girdiler yalnızca
     * {@link #hashOf} gerektiğinde yeniden okunur.
     */
    private record OfficialPalette(String file, IntSet hashes, Set<String> names) {
        static OfficialPalette load(String file) {
            var hashes = new IntOpenHashSet();
            var names = new HashSet<String>();
            for (var entry : readEntries(file)) {
                hashes.add(hash(entry));
                names.add(entry.getString("name"));
            }
            return new OfficialPalette(file, hashes, names);
        }

        int hashOf(String name, Map<String, Object> states) {
            var match = readEntries(file).stream()
                    .filter(entry -> entry.getString("name").equals(name))
                    .filter(entry -> sameStates(entry.getCompound("states"), states))
                    .toList();
            assertEquals(1, match.size(), () -> name + " " + states + " palette tam bir kez bulunmalı");
            return hash(match.getFirst());
        }

        private static List<NbtMap> readEntries(String file) {
            var path = findDataRoot().resolve("protocol_palettes").resolve(file + ".nbt");
            try (var input = NbtUtils.createGZIPReader(Files.newInputStream(path))) {
                var root = (NbtMap) input.readTag();
                return root.getList("blocks", org.cloudburstmc.nbt.NbtType.COMPOUND);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        private static boolean sameStates(NbtMap actual, Map<String, Object> expected) {
            if (!actual.keySet().equals(expected.keySet())) {
                return false;
            }
            for (var entry : expected.entrySet()) {
                var value = actual.get(entry.getKey());
                var wanted = entry.getValue();
                if (value instanceof Number number && wanted instanceof Number other) {
                    if (number.intValue() != other.intValue()) {
                        return false;
                    }
                } else if (wanted instanceof Boolean flag && value instanceof Number number) {
                    if (number.intValue() != (flag ? 1 : 0)) {
                        return false;
                    }
                } else if (!Objects.equals(value, wanted)) {
                    return false;
                }
            }
            return true;
        }

        private static int hash(NbtMap entry) {
            return HashUtils.fnv1a_32_nbt(NbtMap.builder()
                    .putString("name", entry.getString("name"))
                    .putCompound("states", NbtMap.fromMap(new TreeMap<>(entry.getCompound("states"))))
                    .build());
        }

        private static Path findDataRoot() {
            for (var candidate : List.of(Path.of("..", "data", "resources"), Path.of("data", "resources"))) {
                if (Files.isDirectory(candidate.resolve("protocol_palettes"))) {
                    return candidate;
                }
            }
            return fail("data/resources/protocol_palettes bulunamadı");
        }
    }
}
