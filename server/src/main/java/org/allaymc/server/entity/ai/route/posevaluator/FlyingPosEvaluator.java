package org.allaymc.server.entity.ai.route.posevaluator;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.joml.Vector3dc;

/**
 * Ucan varliklar icin havadaki konumlari degerlendirir.
 *
 * <p>Yol bulucu yalnizca dugumun bulundugu blogun carpismasiz oldugunu garanti eder; bu, bir
 * bloktan uzun bir mob icin yeterli degildir — blaze'i tek bloklik bir aralikta gecirip kafasini
 * tavanin icinde birakirdi. Bu yuzden varligin kaplayacagi sutunun tamami burada kontrol
 * ediliyor.</p>
 *
 * <p>Sivilar da reddediliyor. Carpisma sekilleri olmadigi icin bu olmadan blaze dogruca suyun
 * icinden gecirilirdi — ki onu gercekten olduren tek sey odur.</p>
 */
public class FlyingPosEvaluator implements SpacePosEvaluator {

    @Override
    public boolean evaluate(EntityIntelligent entity, Vector3dc pos) {
        var aabb = entity.getAABB();
        var height = aabb.maxY() - aabb.minY();
        // Dugumun kendi blogunun bos oldugu zaten biliniyor; varligin tastigi diger her blogu kontrol et.
        var blocksTall = (int) Math.ceil(height);

        var dimension = entity.getDimension();
        int x = (int) Math.floor(pos.x());
        int y = (int) Math.floor(pos.y());
        int z = (int) Math.floor(pos.z());

        for (int offset = 0; offset < blocksTall; offset++) {
            var blockState = dimension.getBlockState(x, y + offset, z);
            if (blockState == null) {
                return false;
            }

            var blockType = blockState.getBlockType();
            if (blockType.hasBlockTag(BlockTags.WATER) || blockType.hasBlockTag(BlockTags.LAVA)) {
                return false;
            }

            if (offset > 0 && blockState.getBlockStateData().hasCollision()) {
                return false;
            }
        }

        return true;
    }
}
