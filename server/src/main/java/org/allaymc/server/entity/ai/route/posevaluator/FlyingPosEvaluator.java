package org.allaymc.server.entity.ai.route.posevaluator;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.joml.Vector3dc;

/**
 * Evaluates midair positions for flying entities.
 *
 * <p>The route finder only guarantees that the block a node sits in has no collision, which is not
 * enough for a mob taller than one block — it would happily route a blaze through a one-block gap
 * and leave its head inside the ceiling. So the whole column the entity occupies is checked here.</p>
 *
 * <p>Liquids are rejected as well. They have no collision shape, so without this a blaze would be
 * routed straight through water, which is the one thing that actually kills it.</p>
 */
public class FlyingPosEvaluator implements SpacePosEvaluator {

    @Override
    public boolean evaluate(EntityIntelligent entity, Vector3dc pos) {
        var aabb = entity.getAABB();
        var height = aabb.maxY() - aabb.minY();
        // The node's own block is already known to be free; check every further block the entity
        // would stick up into.
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
