package org.allaymc.server.block.dispenser;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.dispenser.DispenseBehavior;
import org.allaymc.api.block.dispenser.DispenseResult;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.interfaces.BlockEntityShulkerBox;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemShulkerBoxStack;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.particle.ShootParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.blockentity.component.shulkerbox.BlockEntityShulkerBoxBaseComponentImpl;
import org.allaymc.server.blockentity.impl.BlockEntityImpl;

public class ShulkerBoxDispenseBehavior implements DispenseBehavior {

    @Override
    public DispenseResult dispense(Block block, BlockFace face, ItemStack item) {
        var dimension = block.getDimension();
        var target = block.offsetPos(face);
        var targetType = target.getBlockState().getBlockType();
        if (targetType != BlockTypes.AIR && !targetType.hasBlockTag(BlockTags.REPLACEABLE)) {
            return DispenseResult.fail();
        }

        var below = dimension.getBlockState(BlockFace.DOWN.offsetPos(target.getPosition()));
        var placedFacing = below.getBlockType() == BlockTypes.AIR ? face : BlockFace.UP;
        if (!dimension.setBlockState(target.getPosition(), item.toBlockState())) {
            return DispenseResult.fail();
        }

        var blockEntity = dimension.getBlockEntity(target.getPosition());
        if (blockEntity instanceof BlockEntityShulkerBox shulker && item instanceof ItemShulkerBoxStack shulkerItem) {
            var container = shulker.getContainer();
            for (var entry : shulkerItem.getStoredItems().entrySet()) {
                var stored = entry.getValue();
                if (stored != null && !stored.isEmptyOrAir()) {
                    container.setItemStack(entry.getKey(), stored.copy(), false);
                }
            }
        }
        if (blockEntity != null && item.hasCustomName()) {
            blockEntity.setCustomName(item.getCustomName());
        }
        if (blockEntity instanceof BlockEntityImpl impl
                && impl.getBaseComponent() instanceof BlockEntityShulkerBoxBaseComponentImpl shulkerBase) {
            shulkerBase.setFacing(placedFacing);
            shulkerBase.sendBlockEntityToViewers(true);
        }

        dimension.updateComparatorOutputLevel(target.getPosition());
        var center = MathUtils.center(block.getPosition());
        dimension.addSound(center, SimpleSound.BLOCK_CLICK);
        dimension.addParticle(center, new ShootParticle(face));
        return DispenseResult.success();
    }
}
