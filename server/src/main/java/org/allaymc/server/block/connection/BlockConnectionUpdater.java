package org.allaymc.server.block.connection;

import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.world.Dimension;
import org.joml.Vector3ic;

/**
 * {@link BlockConnectionRules}'u dünyaya bağlar.
 *
 * <p>GearsMC fork (yol haritası Adım 6.9). Çit, cam panel, parmaklık, tuzak ipi ve merdiven bileşenleri üç yerden çağırır:
 * oyuncu yerleştirirken ({@code place}, istemciye ilk giden durum doğru olsun), blok başka yoldan konduktan sonra
 * ({@code afterPlaced}: piston, komut, eklenti {@code setBlockState}'i) ve yatay komşu değişince
 * ({@code onNeighborUpdate}). Yazma yalnızca durum gerçekten değiştiyse yapılır; hesap aynı girdide aynı sonucu verdiği
 * için yazmanın tetiklediği ikinci {@code afterPlaced} bir şey değiştirmez ve güncelleme zinciri kendiliğinden biter.</p>
 */
public final class BlockConnectionUpdater {

    private BlockConnectionUpdater() {
    }

    /** {@code pos}'a konacak {@code state}'in bağlantılarını komşulara göre hesaplar; dünyaya yazmaz. */
    public static BlockState compute(Dimension dimension, Vector3ic pos, BlockState state) {
        return BlockConnectionRules.update(state, face -> {
            var x = pos.x() + face.getOffset().x();
            var z = pos.z() + face.getOffset().z();
            if (!dimension.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                return null;
            }
            return dimension.getBlockState(x, pos.y(), z);
        });
    }

    /** {@code pos}'taki bloğu yeniden hesaplar, değiştiyse yazar. */
    public static void refresh(Dimension dimension, Vector3ic pos) {
        var current = dimension.getBlockState(pos);
        if (!BlockConnectionRules.isConnectionBlock(current)) {
            return;
        }
        var updated = compute(dimension, pos, current);
        if (!updated.equals(current)) {
            dimension.setBlockState(pos, updated);
        }
    }
}
