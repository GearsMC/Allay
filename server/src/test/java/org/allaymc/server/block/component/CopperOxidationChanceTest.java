package org.allaymc.server.block.component;

import org.allaymc.api.block.component.BlockOxidationComponent;
import org.allaymc.api.block.data.OxidationLevel;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AllayTestExtension.class)
class CopperOxidationChanceTest {

    private static final BiFunction<OxidationLevel, Boolean, BlockType<? extends BlockOxidationComponent>> COPPER =
            (level, waxed) -> switch (level) {
                case UNAFFECTED -> waxed ? BlockTypes.WAXED_COPPER : BlockTypes.COPPER_BLOCK;
                case EXPOSED -> waxed ? BlockTypes.WAXED_EXPOSED_COPPER : BlockTypes.EXPOSED_COPPER;
                case WEATHERED -> waxed ? BlockTypes.WAXED_WEATHERED_COPPER : BlockTypes.WEATHERED_COPPER;
                case OXIDIZED -> waxed ? BlockTypes.WAXED_OXIDIZED_COPPER : BlockTypes.OXIDIZED_COPPER;
            };

    private static Dimension dimension() {
        return Server.getInstance().getWorldPool().getGlobalSpawnPoint().dimension();
    }

    private static Vector3i near(int dx, int dy, int dz) {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        return new Vector3i(spawn.x() + dx, spawn.y() + dy, spawn.z() + dz);
    }

    private static void loadChunks(Dimension dimension, Vector3ic... positions) {
        for (var pos : positions) {
            dimension.getChunkManager().getOrLoadChunk(pos.x() >> 4, pos.z() >> 4).join();
            assertNotNull(dimension.getChunkManager().getChunkByDimensionPos(pos.x(), pos.z()));
        }
    }

    private static void clearArea(Dimension dimension, Vector3ic center) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    var x0 = center.x() + x;
                    var z0 = center.z() + z;
                    if (dimension.getChunkManager().getChunkByDimensionPos(x0, z0) == null) {
                        continue;
                    }
                    dimension.setBlockState(x0, center.y() + y, z0, BlockTypes.AIR.getDefaultState());
                }
            }
        }
    }

    private static void place(Dimension dimension, int x, int y, int z, BlockType<?> blockType) {
        dimension.getChunkManager().getOrLoadChunk(x >> 4, z >> 4).join();
        assertTrue(dimension.setBlockState(x, y, z, blockType.getDefaultState()),
                "test blogu konamadi: " + x + " " + y + " " + z);
        assertEquals(blockType, dimension.getBlockState(x, y, z).getBlockType());
    }

    private static float chanceAt(Dimension dimension, Vector3ic pos, OxidationLevel level) {
        var component = new BlockOxidationComponentImpl(level, COPPER);
        var blockState = dimension.getBlockState(pos);
        return component.calculateOxidationChance(new Block(blockState, new Position3i(pos, dimension), 0));
    }

    @Test
    void aLoneCopperBlockUsesTheUnaffectedChanceModifier() {
        var dimension = dimension();
        var center = near(20, 10, 20);
        loadChunks(dimension, center, new Vector3i(center).add(5, 0, 5), new Vector3i(center).add(-5, 0, -5));
        clearArea(dimension, center);

        place(dimension, center.x(), center.y(), center.z(), BlockTypes.COPPER_BLOCK);
        assertEquals(0.75f, chanceAt(dimension, center, OxidationLevel.UNAFFECTED), 1.0e-6f);

        clearArea(dimension, center);
    }

    @Test
    void aSecondCopperBlockOfTheSameLevelLowersTheChance() {
        var dimension = dimension();
        var center = near(20, 10, 40);
        loadChunks(dimension, center, new Vector3i(center).add(5, 0, 5), new Vector3i(center).add(-5, 0, -5));
        clearArea(dimension, center);

        place(dimension, center.x(), center.y(), center.z(), BlockTypes.COPPER_BLOCK);
        place(dimension, center.x() + 1, center.y(), center.z(), BlockTypes.COPPER_BLOCK);

        assertEquals(0.1875f, chanceAt(dimension, center, OxidationLevel.UNAFFECTED), 1.0e-6f);

        clearArea(dimension, center);
    }

    @Test
    void aLessOxidizedNeighborStopsOxidation() {
        var dimension = dimension();
        var center = near(20, 10, 60);
        loadChunks(dimension, center, new Vector3i(center).add(5, 0, 5), new Vector3i(center).add(-5, 0, -5));
        clearArea(dimension, center);

        place(dimension, center.x(), center.y(), center.z(), BlockTypes.EXPOSED_COPPER);
        place(dimension, center.x() + 2, center.y(), center.z(), BlockTypes.COPPER_BLOCK);

        assertEquals(0f, chanceAt(dimension, center, OxidationLevel.EXPOSED), 1.0e-6f);

        clearArea(dimension, center);
    }

    @Test
    void neighborsOutsideTheManhattanRangeAreIgnored() {
        var dimension = dimension();
        var center = near(20, 10, 80);
        loadChunks(dimension, center, new Vector3i(center).add(5, 0, 5), new Vector3i(center).add(-5, 0, -5));
        clearArea(dimension, center);

        place(dimension, center.x(), center.y(), center.z(), BlockTypes.EXPOSED_COPPER);
        place(dimension, center.x() + 3, center.y() + 3, center.z(), BlockTypes.COPPER_BLOCK);

        assertEquals(1f, chanceAt(dimension, center, OxidationLevel.EXPOSED), 1.0e-6f);

        clearArea(dimension, center);
    }

    @Test
    void waxedNeighborsAreIgnored() {
        var dimension = dimension();
        var center = near(20, 10, 100);
        loadChunks(dimension, center, new Vector3i(center).add(5, 0, 5), new Vector3i(center).add(-5, 0, -5));
        clearArea(dimension, center);

        place(dimension, center.x(), center.y(), center.z(), BlockTypes.COPPER_BLOCK);
        place(dimension, center.x() + 1, center.y(), center.z(), BlockTypes.WAXED_COPPER);

        assertEquals(0.75f, chanceAt(dimension, center, OxidationLevel.UNAFFECTED), 1.0e-6f);

        clearArea(dimension, center);
    }
}
