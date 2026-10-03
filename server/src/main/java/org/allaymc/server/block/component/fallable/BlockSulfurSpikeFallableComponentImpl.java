package org.allaymc.server.block.component.fallable;

import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.sound.BlockBreakSound;
import org.joml.Vector3d;
import org.joml.Vector3ic;

public class BlockSulfurSpikeFallableComponentImpl extends BlockFallableComponentImpl {

    protected static final float FALL_DAMAGE_PER_BLOCK = 1.0f;
    protected static final float MAX_FALL_DAMAGE = 40.0f;
    protected static final double FALLING_BLOCK_SIZE = 0.98;

    public BlockSulfurSpikeFallableComponentImpl() {
        super(null);
    }

    @Override
    protected void tryFall(Dimension dimension, Vector3ic pos, BlockState blockState) {
    }

    @Override
    public void onLanded(Location3d location, double fallDistance, BlockState blockState) {
        var dimension = location.dimension();
        dimension.dropItem(blockState.toItemStack(), location);
        var soundPos = new Vector3d(
                Math.floor(location.x() - FALLING_BLOCK_SIZE / 2) + 0.5,
                Math.floor(location.y() + FALLING_BLOCK_SIZE) + 0.5,
                Math.floor(location.z() - FALLING_BLOCK_SIZE / 2) + 0.5
        );
        dimension.addSound(soundPos, new BlockBreakSound(blockState));
    }

    @Override
    public float calculateDamage(double fallDistance) {
        var fallenBlocks = Math.round(fallDistance) - 1;
        if (fallenBlocks <= 0) {
            return 0;
        }
        return Math.min(fallenBlocks * FALL_DAMAGE_PER_BLOCK, MAX_FALL_DAMAGE);
    }
}
