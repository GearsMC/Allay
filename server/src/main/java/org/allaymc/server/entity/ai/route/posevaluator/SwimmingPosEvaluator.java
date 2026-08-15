package org.allaymc.server.entity.ai.route.posevaluator;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.joml.Vector3dc;

/**
 * Su altindaki konumlari yuzen varliklar icin degerlendirir.
 *
 * <p>{@link FlyingPosEvaluator}'un tersi: orada su reddediliyordu, burada su <em>sart</em>. Yol
 * bulucu yalnizca blogun carpismasiz oldugunu garanti ediyor ve hava da carpismasiz oldugu icin bu
 * kontrol olmasa bir balik havuzdan cikip havada suzulerek gidebilirdi.</p>
 */
public class SwimmingPosEvaluator implements SpacePosEvaluator {

    @Override
    public boolean evaluate(EntityIntelligent entity, Vector3dc pos) {
        var blockState = entity.getDimension().getBlockState(
                (int) Math.floor(pos.x()),
                (int) Math.floor(pos.y()),
                (int) Math.floor(pos.z())
        );
        return blockState != null && blockState.getBlockType().hasBlockTag(BlockTags.WATER);
    }
}
