package org.allaymc.server.block.component.piston;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.world.Dimension;
import org.allaymc.server.block.type.PreservedBlockState;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AllayTestExtension.class)
class PistonPushCalculatorTest {

    private static final Vector3i PISTON_POS = new Vector3i(0, 64, 0);

    @Test
    void testOrdinaryBlockIsPushed() {
        var calculator = new PistonPushCalculator(dimensionWithBlockAbovePiston(BlockTypes.STONE.getDefaultState()), PISTON_POS, BlockFace.UP, false, true);

        assertTrue(calculator.calculate());
        assertEquals(1, calculator.getBlocksToMove().size());
    }

    /**
     * Kimliği bilinmeyen blok pistonla itilmemeli. Taşınan blok hareket bitene kadar yalnızca ad ve durumlarıyla blok
     * varlığında saklanıyor; o arada kayıt alınıp sunucu kapanırsa korunan özgün veri kaybolurdu.
     */
    @Test
    void testPreservedBlockStateIsNotPushed() {
        var preserved = PreservedBlockState.of(NbtMap.builder()
                .putString("name", "minecraft:gears_test_future_block")
                .putCompound("states", NbtMap.EMPTY)
                .putInt("version", Integer.MAX_VALUE)
                .build());
        var calculator = new PistonPushCalculator(dimensionWithBlockAbovePiston(preserved), PISTON_POS, BlockFace.UP, false, true);

        assertFalse(calculator.calculate());
    }

    private static Dimension dimensionWithBlockAbovePiston(BlockState blockState) {
        var dimension = mock(Dimension.class);
        when(dimension.getBlockState(any(Vector3ic.class))).thenAnswer(invocation -> {
            Vector3ic pos = invocation.getArgument(0);
            return pos.x() == 0 && pos.y() == 65 && pos.z() == 0 ? blockState : BlockTypes.AIR.getDefaultState();
        });
        return dimension;
    }
}
