package org.allaymc.server.network.protocol;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.allaymc.api.block.property.enums.MinecraftCorner;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
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
        for (int version : List.of(2192, 2193)) {
            var encoder = international(version).getEncoder();
            for (var state : vanillaStates) {
                assertEquals(state.blockStateHash(), encoder.networkBlockId(state), () -> "v" + version + ": " + state);
            }
        }
    }

    /**
     * Sunucu verisi 26.50'de. 26.40 ve öncesi istemciler merdiven köşesini ve çit/panel bağlantılarını tanımıyor: bu
     * durumlar atılıp aynı bloğun eski çeşidine gider; o sürümde hiç olmayan tür bilinmeyen bloğa düşer.
     */
    @Test
    void olderProtocolsDropStatesAddedIn2650() {
        var v2169 = international(2169).getEncoder();
        var v2169Palette = palette("1_26_40");

        var cornerStairs = BlockTypes.OAK_STAIRS.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER, MinecraftCorner.OUTER_LEFT)
                .setPropertyValue(BlockPropertyTypes.UPSIDE_DOWN_BIT, true)
                .setPropertyValue(BlockPropertyTypes.WEIRDO_DIRECTION, 2);
        assertEquals(v2169Palette.hashOf("minecraft:oak_stairs", Map.of("upside_down_bit", 1, "weirdo_direction", 2)),
                v2169.networkBlockId(cornerStairs));

        var connectedFence = BlockTypes.OAK_FENCE.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH, true)
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_EAST, true);
        assertEquals(v2169Palette.hashOf("minecraft:oak_fence", Map.of()), v2169.networkBlockId(connectedFence));

        assertEquals(UNKNOWN_BLOCK_ID, v2169.networkBlockId(BlockTypes.WHITE_WOOL_STAIRS.getDefaultState()));
        assertNotEquals(UNKNOWN_BLOCK_ID, v2169.networkBlockId(BlockTypes.POPLAR_PLANKS.getDefaultState()));
        assertEquals(UNKNOWN_BLOCK_ID, international(1001).getEncoder().networkBlockId(BlockTypes.POPLAR_PLANKS.getDefaultState()));
    }

    /**
     * 26.50'nin 98 yeni bloğu (yün/beton merdiven ve yarım blokları, {@code red_shrub}, {@code shelf_mushroom}) veri güdümlü:
     * istemci onları ancak sunucu {@code StartGame}'de tanımlarını gönderirse çizer (BDS ve Geyser gönderiyor). Eski
     * istemcide bu bloklar bilinmeyen blok olduğu için tanım gönderilmez.
     */
    @Test
    void dataDrivenVanillaBlocksAreAdvertisedOnlyTo2650Clients() {
        for (int version : List.of(2192, 2193)) {
            var names = international(version).getData().customBlockProperties().stream()
                    .map(property -> property.name())
                    .filter(name -> name.startsWith("minecraft:"))
                    .toList();
            assertEquals(98, names.size(), "v" + version);
            for (var name : names) {
                var blockType = Registries.BLOCKS.get(new org.allaymc.api.utils.identifier.Identifier(name));
                assertNotNull(blockType, name + " Allay kaydında yok");
                assertEquals(blockType.getDefaultState().blockStateHash(),
                        international(version).getEncoder().networkBlockId(blockType.getDefaultState()), name);
            }
        }
        assertTrue(international(2169).getData().customBlockProperties().stream()
                .noneMatch(property -> property.name().startsWith("minecraft:")));
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
        var fence = BlockTypes.OAK_FENCE.getDefaultState().setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH, true);
        var position = new Vector3i(1, 64, 1);

        var v1001 = international(1001).getEncoder();
        assertNotEquals(fence.blockStateHash(), v1001.networkBlockId(fence));
        assertEquals(v1001.networkBlockId(fence), v1001.encodeBlockUpdate(position, 0, fence).getDefinition().runtimeId());

        var v2193 = international(2193).getEncoder();
        assertEquals(fence.blockStateHash(), v2193.encodeBlockUpdate(position, 0, fence).getDefinition().runtimeId());
    }

    @Test
    void creativeBlockItemsUseTheNetworkIdOfTheTargetProtocol() {
        var protocol = international(1001);
        int fenceId = protocol.getEncoder().networkBlockId(BlockTypes.OAK_FENCE.getDefaultState());
        assertNotEquals(BlockTypes.OAK_FENCE.getDefaultState().blockStateHash(), fenceId);

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
        var encoder = international(1001).getEncoder();
        assertNotEquals(fence.blockStateHash(), encoder.networkBlockId(fence));

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
