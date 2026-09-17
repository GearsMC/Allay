package org.allaymc.server.block.connection;

import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.utils.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Çitin ve cam panelin/parmaklığın bir blok durumunun hangi yatay yüzüne bağlandığını söyleyen vanilla tablosu
 * ({@code block_connection_faces.json}).
 *
 * <p>GearsMC fork: 26.50 bağlantı durumları için eklendi (yol haritası Adım 6.8a). "Dolu yüz" Allay'in şekil verisinden
 * türetilemiyor — ruh kumu, çamur ve bal bloğu bağlanıyor; kaktüs, yaprak ve üst basamak bağlanmıyor; iç köşeli
 * merdivenin yan yüzü Java'nın aksine bağlanmıyor. Tablo BDS'te her durumun dört yanına sonda konarak ölçüldü, üretimi
 * ve kuralları {@code data/.../importer/ConnectionFaceImport}.</p>
 *
 * <p>Aile bağlantıları (çitten çite, panelden duvara...) tabloda yoktur; onları {@link BlockConnectionRules} verir.</p>
 */
public final class BlockConnectionFaces {

    private static final String RESOURCE = "block_connection_faces.json";

    private BlockConnectionFaces() {
    }

    /**
     * @param state bağlanılacak blok
     * @param face  o bloğun bağlanılan yüzü (çitin bakış yönünün tersi); yalnızca yatay yüzler
     * @return çit ve panel bu yüze bağlanıyorsa {@code true}
     */
    public static boolean isConnectable(BlockState state, BlockFace face) {
        return (Holder.MASKS.get(state.blockStateHash()) & bit(face)) != 0;
    }

    private static int bit(BlockFace face) {
        return switch (face) {
            case NORTH -> 1;
            case EAST -> 1 << 1;
            case SOUTH -> 1 << 2;
            case WEST -> 1 << 3;
            default -> throw new IllegalArgumentException("Yatay olmayan yüz: " + face);
        };
    }

    // Tablo ilk sorguda yüklenir: sınıf blok kayıt defterinden önce yüklenebilir, veri ise yalnızca hash'e bakar.
    private static final class Holder {
        static final Int2IntOpenHashMap MASKS = load();

        private static Int2IntOpenHashMap load() {
            var masks = new Int2IntOpenHashMap();
            try (var reader = new InputStreamReader(new BufferedInputStream(Utils.getResource(RESOURCE)), StandardCharsets.UTF_8)) {
                for (var entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    var mask = Integer.parseInt(entry.getKey());
                    entry.getValue().getAsJsonArray().forEach(hash -> masks.put(hash.getAsInt(), mask));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return masks;
        }
    }
}
