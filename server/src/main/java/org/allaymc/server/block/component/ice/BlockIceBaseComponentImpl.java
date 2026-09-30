package org.allaymc.server.block.component.ice;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.component.BlockLiquidBaseComponent;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.eventbus.event.block.BlockFadeEvent;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.block.component.BlockBaseComponentImpl;
import org.joml.Vector3ic;

import java.util.Set;

/**
 * @author IWareQ
 */
public class BlockIceBaseComponentImpl extends BlockBaseComponentImpl {
    public BlockIceBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    /**
     * SkyBuild {@code Ice::onRandomTick}: buz isiktan degil yalnizca yanindaki lavadan erir.
     * Su olusamayan boyutta (nether) erime hava + fizz sesi + duman birakir.
     */
    @Override
    public void onRandomUpdate(Block block) {
        super.onRandomUpdate(block);

        var dimension = block.getDimension();
        var pos = block.getPosition();
        if (!isAdjacentToLava(dimension, pos)) {
            return;
        }

        var waterExists = BlockLiquidBaseComponent.canWaterExistAt(dimension, pos);
        var event = new BlockFadeEvent(block, waterExists
                ? BlockTypes.WATER.getDefaultState() : BlockTypes.AIR.getDefaultState());
        if (!event.call()) {
            return;
        }
        dimension.setBlockState(pos, event.getNewBlockState());
        if (!waterExists) {
            playEvaporateEffects(dimension, pos);
        }
    }

    protected boolean isAdjacentToLava(Dimension dimension, Vector3ic pos) {
        for (var face : BlockFace.VALUES) {
            if (BlockTypes.LAVA.getBlockBehavior()
                    .isSameLiquidType(dimension.getBlockState(face.offsetPos(pos)).getBlockType())) {
                return true;
            }
        }

        return false;
    }

    private static void playEvaporateEffects(Dimension dimension, Vector3ic pos) {
        var center = MathUtils.center(pos);
        dimension.addSound(center, SimpleSound.FIZZ);
        dimension.addParticle(center.x(), center.y(), center.z(), SimpleParticle.SMOKE);
    }

    @Override
    public Set<ItemStack> getDrops(Block block, ItemStack usedItem, Entity entity) {
        // SkyBuild Ice::onBreak: su olusabilen yerde kirilan buz su birakir; nether'de buharlasir.
        var dimension = block.getDimension();
        var pos = block.getPosition();
        if (BlockLiquidBaseComponent.canWaterExistAt(dimension, pos)) {
            if (block.offsetPos(BlockFace.DOWN).getBlockType() != BlockTypes.AIR) {
                dimension.setBlockState(pos, BlockTypes.WATER.getDefaultState());
            }
        } else {
            playEvaporateEffects(dimension, pos);
        }

        return Set.of();
    }

    @Override
    public boolean canRandomUpdate() {
        return true;
    }
}
