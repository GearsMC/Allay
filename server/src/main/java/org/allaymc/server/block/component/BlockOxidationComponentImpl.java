package org.allaymc.server.block.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.allaymc.api.block.component.BlockBaseComponent;
import org.allaymc.api.block.component.BlockOxidationComponent;
import org.allaymc.api.block.data.OxidationLevel;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.block.BlockFadeEvent;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.server.block.component.event.CBlockRandomUpdateEvent;
import org.allaymc.server.block.component.event.CBlockTryRandomUpdateEvent;
import org.allaymc.server.component.annotation.Dependency;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

/**
 * @author IWareQ
 */
@Getter
@RequiredArgsConstructor
public class BlockOxidationComponentImpl implements BlockOxidationComponent {
    @Identifier.Component
    public static final Identifier IDENTIFIER = new Identifier("minecraft:block_oxidation_component");

    private static final float OXIDATION_ATTEMPT_CHANCE = 64f / 1125f;
    private static final int SCAN_RANGE = 4;

    private final OxidationLevel oxidationLevel;
    private final BiFunction<OxidationLevel, Boolean, BlockType<? extends BlockOxidationComponent>> blockTypeFunction;

    @Dependency
    private BlockBaseComponent baseComponent;

    @EventHandler
    protected void onBlockRandomUpdate(CBlockRandomUpdateEvent event) {
        var random = ThreadLocalRandom.current();
        if (random.nextFloat() >= OXIDATION_ATTEMPT_CHANCE) {
            return;
        }

        var current = event.getBlock();
        var chance = calculateOxidationChance(current);
        if (chance <= 0 || random.nextFloat() >= chance) {
            return;
        }

        var nextBlockType = getBlockWithOxidationLevel(OxidationLevel.values()[this.oxidationLevel.ordinal() + 1]);
        var blockFadeEvent = new BlockFadeEvent(current, nextBlockType.copyPropertyValuesFrom(current.getBlockState()));
        if (blockFadeEvent.call()) {
            current.getDimension().setBlockState(current.getPosition(), blockFadeEvent.getNewBlockState());
        }
    }

    protected float calculateOxidationChance(Block current) {
        var currentLevel = this.oxidationLevel.ordinal();

        int higherOxidizedBlocks = 0;
        int sameOxidizedBlocks = 0;
        for (int x = -SCAN_RANGE; x <= SCAN_RANGE; x++) {
            for (int y = -SCAN_RANGE; y <= SCAN_RANGE; y++) {
                for (int z = -SCAN_RANGE; z <= SCAN_RANGE; z++) {
                    if ((x == 0 && y == 0 && z == 0) || Math.abs(x) + Math.abs(y) + Math.abs(z) > SCAN_RANGE) {
                        continue;
                    }

                    var neighbor = current.offsetPos(x, y, z);
                    if (!(neighbor.getBehavior() instanceof BlockOxidationComponent neighborOxidation) || neighborOxidation.isWaxed()) {
                        continue;
                    }

                    var neighborLevel = neighborOxidation.getOxidationLevel().ordinal();
                    if (neighborLevel < currentLevel) {
                        return 0;
                    } else if (neighborLevel > currentLevel) {
                        higherOxidizedBlocks++;
                    } else {
                        sameOxidizedBlocks++;
                    }
                }
            }
        }

        var chance = (higherOxidizedBlocks + 1f) / (higherOxidizedBlocks + sameOxidizedBlocks + 1f);
        return chance * chance * (currentLevel == OxidationLevel.UNAFFECTED.ordinal() ? 0.75f : 1f);
    }

    @Override
    public BlockType<? extends BlockOxidationComponent> getBlockWithOxidationLevel(OxidationLevel oxidationLevel) {
        return blockTypeFunction.apply(oxidationLevel, isWaxed());
    }

    @Override
    public boolean isWaxed() {
        return baseComponent.getBlockType().getIdentifier().path().startsWith("waxed");
    }

    @Override
    public BlockType<? extends BlockOxidationComponent> getBlockWithWaxed(boolean waxed) {
        return blockTypeFunction.apply(this.oxidationLevel, waxed);
    }

    @EventHandler
    protected void onTryRandomUpdate(CBlockTryRandomUpdateEvent event) {
        event.canRandomUpdate(canOxidate());
    }
}
