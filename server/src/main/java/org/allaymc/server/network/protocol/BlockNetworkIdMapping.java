package org.allaymc.server.network.protocol;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.utils.Utils;
import org.allaymc.api.utils.hash.HashUtils;
import org.allaymc.api.utils.identifier.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sunucu blok durumlarını bir istemci sürümünün tanıdığı ağ kimliklerine çevirir.
 *
 * <p>GearsMC fork: Allay'in blok verisi tek bir oyun sürümünü izler, istemciler ise 818–2193 arasında değişir.
 * Kimlikler hash'li gittiği için istemci bir bloğu ancak ad + durum hash'i kendi paletinde varsa çizer. 26.50
 * merdivenlere {@code minecraft:corner}, çit/panel/parmaklık/tuzak ipine {@code minecraft:connection_*} ekleyince
 * 26.30 verisiyle giden bu bloklar 26.50 istemcisinde görünmez olmuştu.</p>
 *
 * <p>Eşleme, protokolün resmi paletinden ({@code protocol_palettes/}) bir kez kurulur. Her sunucu durumu için:</p>
 * <ol>
 *     <li>hash istemci paletinde varsa aynen gider;</li>
 *     <li>blok türü istemcide varsa istemcinin bilmediği özellikler atılır, eksik özellikler istemci paletinde o
 *     türün ilk çeşidinden alınır (26.50 için {@code corner=none}, bağlantılar 0);</li>
 *     <li>tür istemcide hiç yoksa {@link #UNKNOWN_BLOCK_ID} gider.</li>
 * </ol>
 * <p>Resmi paletlerde yalnızca vanilla bloklar bulunur. {@code minecraft} dışındaki ad alanlarındaki bloklar (eklenti
 * blokları, özel blok tanımı olsun olmasın) eşlenmez, kimlikleri olduğu gibi gider.</p>
 * <p>Yalnızca farklı olan kimlikler saklanır; sunucu verisiyle aynı paleti kullanan protokolde tablo boş kalır.</p>
 */
@Slf4j
public final class BlockNetworkIdMapping {
    /**
     * İstemcinin {@code minecraft:unknown} için kullandığı özel kimlik (bkz. {@link HashUtils#computeBlockStateHash}).
     */
    public static final int UNKNOWN_BLOCK_ID = -2;

    private static final BlockNetworkIdMapping IDENTITY = new BlockNetworkIdMapping(Int2IntMaps.EMPTY_MAP);
    private static final Map<String, ClientPalette> PALETTE_CACHE = new ConcurrentHashMap<>();

    private final Int2IntMap overrides;

    private BlockNetworkIdMapping(Int2IntMap overrides) {
        this.overrides = overrides;
    }

    /**
     * Resmi paleti olmayan protokoller için kimlikleri olduğu gibi bırakan eşleme.
     */
    public static BlockNetworkIdMapping identity() {
        return IDENTITY;
    }

    /**
     * Verilen sunucu durumlarını bir resmi palete göre eşler.
     *
     * @param paletteResource sınıf yolundaki gzip'li palet ({@code protocol_palettes/1_26_50.nbt} gibi)
     * @param serverStates    sunucu durumları; vanilla olmayanlar olduğu gibi bırakılır
     * @return eşleme
     */
    public static BlockNetworkIdMapping fromPalette(String paletteResource, Collection<BlockState> serverStates) {
        Objects.requireNonNull(paletteResource, "paletteResource");
        var palette = PALETTE_CACHE.computeIfAbsent(paletteResource, ClientPalette::load);
        var overrides = new Int2IntOpenHashMap();
        int unresolved = 0;
        for (var state : serverStates) {
            var identifier = state.getBlockType().getIdentifier();
            int hash = state.blockStateHash();
            if (!Identifier.DEFAULT_NAMESPACE.equals(identifier.namespace())
                || hash == UNKNOWN_BLOCK_ID
                || palette.hashes().contains(hash)) {
                continue;
            }

            var name = identifier.toString();
            var clientStates = palette.firstStates().get(name);
            if (clientStates == null) {
                overrides.put(hash, UNKNOWN_BLOCK_ID);
                continue;
            }

            var serverValues = new HashMap<String, Object>();
            state.getPropertyValues().forEach((type, value) -> serverValues.put(type.getName(), value.getSerializedValue()));
            var adapted = new TreeMap<String, Object>();
            for (var entry : clientStates.entrySet()) {
                adapted.put(entry.getKey(), serverValues.getOrDefault(entry.getKey(), entry.getValue()));
            }

            int adaptedHash = hash(name, adapted);
            if (palette.hashes().contains(adaptedHash)) {
                overrides.put(hash, adaptedHash);
            } else {
                overrides.put(hash, UNKNOWN_BLOCK_ID);
                unresolved++;
            }
        }

        if (unresolved > 0) {
            log.warn("{} paletinde karşılığı bulunamayan {} blok durumu bilinmeyen blok olarak gönderilecek", paletteResource, unresolved);
        }
        return overrides.isEmpty() ? IDENTITY : new BlockNetworkIdMapping(overrides);
    }

    /**
     * Bir sunucu durumunun bu protokoldeki ağ kimliğini döndürür.
     */
    public int networkId(BlockState blockState) {
        int hash = blockState.blockStateHash();
        return overrides.getOrDefault(hash, hash);
    }

    private static int hash(String name, Map<String, Object> states) {
        return HashUtils.fnv1a_32_nbt(NbtMap.builder()
                .putString("name", name)
                .putCompound("states", NbtMap.fromMap(states instanceof TreeMap ? states : new TreeMap<>(states)))
                .build());
    }

    /**
     * Resmi paletten yalnızca eşleme için gerekenler: bütün durum hash'leri ve her türün ilk çeşidinin durumları.
     */
    private record ClientPalette(IntSet hashes, Map<String, NbtMap> firstStates) {
        static ClientPalette load(String resource) {
            try (var input = NbtUtils.createGZIPReader(Utils.getResource(resource))) {
                var root = (NbtMap) input.readTag();
                var hashes = new IntOpenHashSet();
                var firstStates = new HashMap<String, NbtMap>();
                for (var entry : root.getList("blocks", NbtType.COMPOUND)) {
                    var name = entry.getString("name");
                    var states = entry.getCompound("states");
                    hashes.add(hash(name, states));
                    firstStates.putIfAbsent(name, states);
                }
                return new ClientPalette(hashes, Map.copyOf(firstStates));
            } catch (IOException exception) {
                throw new UncheckedIOException("Palet okunamadı: " + resource, exception);
            }
        }
    }
}
