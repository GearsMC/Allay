package org.allaymc.server.block.component;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.enums.DripstoneThickness;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.chunk.FakeChunkLoader;
import org.allaymc.server.block.component.fallable.BlockSulfurSpikeFallableComponentImpl;
import org.allaymc.server.block.impl.BlockBehaviorImpl;
import org.allaymc.server.world.manager.AllayBlockUpdateManager;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(AllayTestExtension.class)
class BlockSulfurSpikeTest {

    private static final AtomicInteger NEXT_SITE = new AtomicInteger();

    private Dimension dimension;
    private Vector3i origin;

    @BeforeEach
    void setUp() {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        dimension = spawn.dimension();
        var site = NEXT_SITE.getAndIncrement();
        origin = new Vector3i((int) spawn.x() - 160 - site * 8, (int) spawn.y() + 48, (int) spawn.z() + 160);
        for (var dx = -2; dx <= 2; dx++) {
            for (var dy = -3; dy <= 14; dy++) {
                for (var dz = -2; dz <= 2; dz++) {
                    var x = origin.x + dx;
                    var y = origin.y + dy;
                    var z = origin.z + dz;
                    dimension.getChunkManager().getOrLoadChunk(x >> 4, z >> 4).join();
                    dimension.setBlockState(x, y, z, BlockTypes.AIR.getDefaultState());
                }
            }
        }
        settle();
    }

    @Test
    void placingOnFloorMakesStandingTip() {
        solid(0, -1);
        assertTrue(place(0, BlockFace.UP));

        assertSpike(0, false, DripstoneThickness.TIP);
    }

    @Test
    void placingUnderCeilingMakesHangingTip() {
        solid(0, 1);
        assertTrue(place(0, BlockFace.DOWN));

        assertSpike(0, true, DripstoneThickness.TIP);
    }

    @Test
    void placingWithoutAnythingAboveOrBelowIsRejected() {
        assertFalse(place(0, BlockFace.NORTH));
        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void stalactiteThicknessFollowsColumnLength() {
        solid(0, 4);
        place(3, BlockFace.DOWN);
        place(2, BlockFace.DOWN);
        assertSpike(3, true, DripstoneThickness.FRUSTUM);
        assertSpike(2, true, DripstoneThickness.TIP);

        place(1, BlockFace.DOWN);
        assertSpike(3, true, DripstoneThickness.BASE);
        assertSpike(2, true, DripstoneThickness.FRUSTUM);
        assertSpike(1, true, DripstoneThickness.TIP);

        place(0, BlockFace.DOWN);
        assertSpike(3, true, DripstoneThickness.BASE);
        assertSpike(2, true, DripstoneThickness.MIDDLE);
        assertSpike(1, true, DripstoneThickness.FRUSTUM);
        assertSpike(0, true, DripstoneThickness.TIP);
    }

    @Test
    void stalagmiteThicknessFollowsColumnLength() {
        solid(0, -1);
        place(0, BlockFace.UP);
        place(1, BlockFace.UP);
        place(2, BlockFace.UP);

        assertSpike(0, false, DripstoneThickness.BASE);
        assertSpike(1, false, DripstoneThickness.FRUSTUM);
        assertSpike(2, false, DripstoneThickness.TIP);
    }

    @Test
    void fillingTheGapMergesStalactiteAndStalagmite() {
        solid(0, -2);
        solid(0, 2);
        place(-1, BlockFace.UP);
        place(1, BlockFace.DOWN);

        place(0, BlockFace.NORTH);

        assertSpike(0, false, DripstoneThickness.MERGE);
        assertSpike(1, true, DripstoneThickness.MERGE);
    }

    @Test
    void sideFacePlacementOverFloorFallsOnNextTick() {
        solid(0, -1);
        assertTrue(placeWithoutSettling(0, BlockFace.NORTH));
        assertSpike(0, true, DripstoneThickness.TIP);

        settle();

        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void stalagmiteBreaksWhenFloorIsRemoved() {
        solid(0, -1);
        place(0, BlockFace.UP);

        dimension.setBlockState(origin.x, origin.y - 1, origin.z, BlockTypes.AIR.getDefaultState());
        settle();

        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void stalactiteFallsWhenCeilingIsRemoved() {
        solid(0, 3);
        place(2, BlockFace.DOWN);
        place(1, BlockFace.DOWN);
        place(0, BlockFace.DOWN);

        dimension.setBlockState(origin.x, origin.y + 3, origin.z, BlockTypes.AIR.getDefaultState());
        settle();

        assertEquals(BlockTypes.AIR, state(2).getBlockType());
        assertEquals(BlockTypes.AIR, state(1).getBlockType());
        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void breakingInsideStalactiteRebuildsTheAttachedPartAndDropsTheRest() {
        solid(0, 4);
        place(3, BlockFace.DOWN);
        place(2, BlockFace.DOWN);
        place(1, BlockFace.DOWN);
        place(0, BlockFace.DOWN);

        dimension.breakBlock(new Vector3i(origin.x, origin.y + 1, origin.z), null, null);
        settle();

        assertSpike(3, true, DripstoneThickness.FRUSTUM);
        assertSpike(2, true, DripstoneThickness.TIP);
        assertEquals(BlockTypes.AIR, state(1).getBlockType());
        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void breakingMergeTurnsThePartnerIntoTip() {
        solid(0, -2);
        solid(0, 2);
        place(-1, BlockFace.UP);
        place(1, BlockFace.DOWN);
        place(0, BlockFace.NORTH);

        dimension.breakBlock(new Vector3i(origin.x, origin.y, origin.z), null, null);
        settle();

        assertEquals(BlockTypes.AIR, state(0).getBlockType());
        assertSpike(1, true, DripstoneThickness.TIP);
        assertSpike(-1, false, DripstoneThickness.TIP);
    }

    @Test
    void stalactiteUnderSulfurGrowsDownward() {
        dimension.setBlockState(origin.x, origin.y + 2, origin.z, BlockTypes.SULFUR.getDefaultState());
        place(1, BlockFace.DOWN);

        component().tryGrow(dimension, origin.x, origin.y + 1, origin.z);
        settle();

        assertSpike(1, true, DripstoneThickness.FRUSTUM);
        assertSpike(0, true, DripstoneThickness.TIP);
    }

    @Test
    void stalactiteUnderOtherBlocksDoesNotGrow() {
        solid(0, 2);
        place(1, BlockFace.DOWN);

        component().tryGrow(dimension, origin.x, origin.y + 1, origin.z);

        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void stalagmiteGrowsOnlyTowardAHangingTip() {
        solid(0, -1);
        place(0, BlockFace.UP);

        component().tryGrow(dimension, origin.x, origin.y, origin.z);
        assertEquals(BlockTypes.AIR, state(1).getBlockType());

        solid(0, 6);
        place(5, BlockFace.DOWN);
        component().tryGrow(dimension, origin.x, origin.y, origin.z);
        settle();

        assertSpike(0, false, DripstoneThickness.FRUSTUM);
        assertSpike(1, false, DripstoneThickness.TIP);
    }

    @Test
    void columnStopsGrowingAtSevenBlocks() {
        dimension.setBlockState(origin.x, origin.y + 8, origin.z, BlockTypes.SULFUR.getDefaultState());
        for (var dy = 7; dy >= 1; dy--) {
            place(dy, BlockFace.DOWN);
        }

        component().tryGrow(dimension, origin.x, origin.y + 1, origin.z);

        assertEquals(BlockTypes.AIR, state(0).getBlockType());
    }

    @Test
    void fallDamageMatchesPocketMine() {
        var fallable = new BlockSulfurSpikeFallableComponentImpl();
        assertEquals(0, fallable.calculateDamage(1.0));
        assertEquals(0, fallable.calculateDamage(1.4));
        assertEquals(1, fallable.calculateDamage(2.4));
        assertEquals(2, fallable.calculateDamage(2.5));
        assertEquals(9, fallable.calculateDamage(10.0));
        assertEquals(40, fallable.calculateDamage(80.0));
    }

    @Test
    void fallingStalactiteLandsAsAnItemOnRealTicks() throws InterruptedException {
        solid(0, -1);
        solid(0, 7);
        place(6, BlockFace.DOWN);
        place(5, BlockFace.DOWN);

        var loader = new FakeChunkLoader(() -> new Location3d(origin.x, origin.y, origin.z, dimension), 1);
        dimension.getChunkManager().addChunkLoader(loader);
        try {
            assertTrue(waitFor(() -> dimension.getChunkManager().isChunkActive(origin.x >> 4, origin.z >> 4), 5000), "chunk aktif olmadi");
            dimension.setBlockState(origin.x, origin.y + 7, origin.z, BlockTypes.AIR.getDefaultState());

            assertTrue(waitFor(() -> state(6).getBlockType() == BlockTypes.AIR && state(5).getBlockType() == BlockTypes.AIR, 3000), "sarkit dusmedi");
            assertTrue(waitFor(() -> spikeItemsNearOrigin() == 2, 8000), "yere carpan dikitler esyaya donmedi");
            assertEquals(BlockTypes.AIR, state(0).getBlockType());
        } finally {
            dimension.getChunkManager().removeChunkLoader(loader);
        }
    }

    private long spikeItemsNearOrigin() {
        return dimension.getEntities().values().stream()
                .filter(entity -> entity instanceof EntityItem)
                .map(EntityItem.class::cast)
                .filter(item -> item.getItemStack().getItemType() == ItemTypes.SULFUR_SPIKE)
                .filter(item -> item.getLocation().distance(origin.x + 0.5, origin.y, origin.z + 0.5) < 3)
                .mapToLong(item -> item.getItemStack().getCount())
                .sum();
    }

    private boolean place(int dy, BlockFace face) {
        var placed = placeWithoutSettling(dy, face);
        settle();
        return placed;
    }

    private boolean placeWithoutSettling(int dy, BlockFace face) {
        var pos = new Vector3i(origin.x, origin.y + dy, origin.z);
        var clicked = face.opposite().offsetPos(pos);
        var info = new PlayerInteractInfo(mock(EntityPlayer.class), clicked, new Vector3f(0.5f, 0.5f, 0.5f), face);
        return BlockTypes.SULFUR_SPIKE.getBlockBehavior().place(dimension, BlockTypes.SULFUR_SPIKE.getDefaultState(), pos, info);
    }

    private void solid(int dx, int dy) {
        dimension.setBlockState(origin.x + dx, origin.y + dy, origin.z, BlockTypes.STONE.getDefaultState());
        settle();
    }

    private BlockState state(int dy) {
        return dimension.getBlockState(origin.x, origin.y + dy, origin.z);
    }

    private void assertSpike(int dy, boolean hanging, DripstoneThickness thickness) {
        var state = state(dy);
        assertEquals(BlockTypes.SULFUR_SPIKE, state.getBlockType(), "y+" + dy + " dikit degil");
        assertEquals(hanging, state.getPropertyValue(BlockPropertyTypes.HANGING), "y+" + dy + " yon");
        assertEquals(thickness, state.getPropertyValue(BlockPropertyTypes.DRIPSTONE_THICKNESS), "y+" + dy + " kalinlik");
    }

    private BlockSulfurSpikeBaseComponentImpl component() {
        return (BlockSulfurSpikeBaseComponentImpl) ((BlockBehaviorImpl) BlockTypes.SULFUR_SPIKE.getBlockBehavior()).getBaseComponent();
    }

    private void settle() {
        var blockUpdateManager = (AllayBlockUpdateManager) dimension.getBlockUpdateManager();
        var done = new CompletableFuture<Void>();
        dimension.getScheduler().runLater(dimension, () -> {
            for (var i = 0; i < 8; i++) {
                blockUpdateManager.tick();
            }
            done.complete(null);
        });
        done.orTimeout(5, TimeUnit.SECONDS).join();
    }

    private static boolean waitFor(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        var deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(25);
        }
        return condition.getAsBoolean();
    }
}
