package org.allaymc.api.world.sound;

import org.allaymc.api.block.type.BlockState;

public record BlockBreakSound(BlockState blockState) implements Sound {
}
