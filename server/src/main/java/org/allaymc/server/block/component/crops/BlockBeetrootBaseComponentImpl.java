package org.allaymc.server.block.component.crops;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.server.block.FortuneDropHelper;

import java.util.Set;

import static org.allaymc.api.block.property.type.BlockPropertyTypes.GROWTH;

/**
 * @author daoge_cmd
 */
public class BlockBeetrootBaseComponentImpl extends BlockCropsBaseComponentImpl {
    public BlockBeetrootBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public Set<ItemStack> getDrops(Block block, ItemStack usedItem, Entity entity) {
        var growth = block.getPropertyValue(GROWTH);
        if (growth < GROWTH.getMax()) {
            return Set.of(ItemTypes.BEETROOT_SEEDS.createItemStack());
        }

        // A mature crop always yields exactly one beetroot, plus a variable amount of
        // seeds. The seed amount is the fortune-affected roll, not the beetroot amount.
        var seedCount = FortuneDropHelper.binomial(usedItem, 0);
        if (seedCount <= 0) {
            return Set.of(ItemTypes.BEETROOT.createItemStack());
        }

        return Set.of(ItemTypes.BEETROOT.createItemStack(), ItemTypes.BEETROOT_SEEDS.createItemStack(seedCount));
    }
}
