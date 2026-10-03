package org.allaymc.server.block.component;

import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.property.enums.PotentSulfurState;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.interfaces.BlockEntityPotentSulfur;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityFallingBlock;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.player.Player;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.tuple.Pair;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.chunk.FakeChunkLoader;
import org.allaymc.api.world.manager.EntityManager;
import org.allaymc.api.world.physics.AABBOverlapFilter;
import org.allaymc.api.world.physics.EntityPhysicsEngine;
import org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl;
import org.allaymc.server.blockentity.impl.BlockEntityImpl;
import org.allaymc.server.world.manager.AllayBlockUpdateManager;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl.computeDormantDurationSeconds;
import static org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl.computeDormantValue;
import static org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl.computeEruptionDurationSeconds;
import static org.allaymc.server.blockentity.component.BlockEntityPotentSulfurBaseComponentImpl.computeEruptionValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AllayTestExtension.class)
class BlockPotentSulfurTest {

    private static final AtomicInteger NEXT_SITE = new AtomicInteger();

    private Dimension dimension;
    private Vector3i origin;

    @BeforeEach
    void setUp() {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        dimension = spawn.dimension();
        var site = NEXT_SITE.getAndIncrement();
        origin = new Vector3i((int) spawn.x() + 160 + site * 8, (int) spawn.y() + 48, (int) spawn.z() + 160);
        for (var dx = -2; dx <= 2; dx++) {
            for (var dy = -3; dy <= 7; dy++) {
                for (var dz = -2; dz <= 2; dz++) {
                    var x = origin.x + dx;
                    var y = origin.y + dy;
                    var z = origin.z + dz;
                    dimension.getChunkManager().getOrLoadChunk(x >> 4, z >> 4).join();
                    dimension.setBlockState(x, y, z, BlockTypes.AIR.getDefaultState());
                }
            }
        }
    }

    @Test
    void pocketMineDurationHashesAreReproduced() {
        int[][] cases = {
                {0, 64, 0, 15, 1},
                {1, 2, 3, 25, 1},
                {-5, 70, 12, 22, 1},
                {1234, -30, -987, 28, 1},
                {-20000, 100, 35000, 27, 1},
                {500000, 64, -500000, 15, 1},
                {1900000, 64, 5, 24, 1},
                {3, 65, 7, 18, 2},
                {-1, -1, -1, 24, 2},
                {7, 71, -3, 18, 2},
                {123456, -63, -654321, 21, 2}
        };
        for (var c : cases) {
            assertEquals(c[3], computeDormantValue(c[0], c[1], c[2]), "dormant " + c[0] + "," + c[1] + "," + c[2]);
            assertEquals(c[4], computeEruptionValue(c[0], c[1], c[2]), "eruption " + c[0] + "," + c[1] + "," + c[2]);
        }
        assertEquals(10 * 2 + 25, computeDormantDurationSeconds(3, 1, 2, 3));
        assertEquals(2 + 2, computeEruptionDurationSeconds(3, 3, 65, 7));
    }

    @Test
    void waterAboveTogglesBetweenDryAndWet() {
        build(BlockTypes.STONE.getDefaultState(), 0);
        assertEquals(PotentSulfurState.DRY, state());
        assertInstanceOf(BlockEntityPotentSulfur.class, dimension.getBlockEntity(origin));

        dimension.setBlockState(origin.x, origin.y + 1, origin.z, BlockTypes.WATER.getDefaultState());
        settle();
        assertEquals(PotentSulfurState.WET, state());
        assertTrue(dimension.getBlockUpdateManager().hasScheduledBlockUpdate(origin));

        dimension.setBlockState(origin.x, origin.y + 1, origin.z, BlockTypes.AIR.getDefaultState());
        settle();
        assertEquals(PotentSulfurState.DRY, state());
    }

    @Test
    void flowingWaterDoesNotCountAsColumn() {
        build(BlockTypes.STONE.getDefaultState(), 0);
        var flowing = BlockTypes.FLOWING_WATER.getDefaultState().setPropertyValue(BlockPropertyTypes.LIQUID_DEPTH, 3);
        dimension.setBlockState(origin.x, origin.y + 1, origin.z, flowing);
        settle();
        assertEquals(PotentSulfurState.DRY, state());
    }

    @Test
    void magmaGeyserCyclesBetweenDormantAndErupting() {
        build(BlockTypes.MAGMA.getDefaultState(), 2);
        var dormant = computeDormantDurationSeconds(2, origin.x, origin.y, origin.z);
        var eruption = computeEruptionDurationSeconds(2, origin.x, origin.y, origin.z);

        assertEquals(PotentSulfurState.DORMANT, state());
        assertEquals(dormant, blockEntity().getCountdown());
        assertTrue(dimension.getBlockUpdateManager().hasScheduledBlockUpdate(origin));

        blockEntity().setCountdown(1);
        scheduledUpdate();
        assertEquals(PotentSulfurState.DORMANT, state());
        assertEquals(0, blockEntity().getCountdown());

        scheduledUpdate();
        assertEquals(PotentSulfurState.ERUPTING, state());
        assertEquals(eruption, blockEntity().getCountdown());

        scheduledUpdate();
        assertEquals(PotentSulfurState.ERUPTING, state());
        assertEquals(eruption - 1, blockEntity().getCountdown());

        blockEntity().setCountdown(0);
        scheduledUpdate();
        assertEquals(PotentSulfurState.DORMANT, state());
        assertEquals(dormant, blockEntity().getCountdown());
    }

    @Test
    void neighborUpdateDoesNotResetRunningMagmaCountdown() {
        build(BlockTypes.MAGMA.getDefaultState(), 1);
        blockEntity().setCountdown(7);

        dimension.setBlockState(origin.x + 1, origin.y, origin.z, BlockTypes.STONE.getDefaultState());
        settle();

        assertEquals(PotentSulfurState.DORMANT, state());
        assertEquals(7, blockEntity().getCountdown());
    }

    @Test
    void removingMagmaFallsBackToWet() {
        build(BlockTypes.MAGMA.getDefaultState(), 1);
        assertEquals(PotentSulfurState.DORMANT, state());

        dimension.setBlockState(origin.x, origin.y - 1, origin.z, BlockTypes.STONE.getDefaultState());
        settle();

        assertEquals(PotentSulfurState.WET, state());
        assertEquals(0, blockEntity().getCountdown());
    }

    @Test
    void lavaSourceBelowMakesContinuousGeyser() {
        build(BlockTypes.LAVA.getDefaultState(), 1);

        assertEquals(PotentSulfurState.CONTINUOUS, state());
        assertEquals(0, blockEntity().getCountdown());

        scheduledUpdate();
        assertEquals(PotentSulfurState.CONTINUOUS, state());
    }

    @Test
    void countdownIsSavedAsIntOnlyWhilePositive() {
        build(BlockTypes.STONE.getDefaultState(), 0);
        var blockEntity = blockEntity();

        blockEntity.setCountdown(17);
        assertEquals(17, blockEntity.saveNBT().getInt("countdown"));

        blockEntity.setCountdown(-5);
        assertEquals(0, blockEntity.getCountdown());
        assertFalse(blockEntity.saveNBT().containsKey("countdown"));

        blockEntity.loadNBT(blockEntity.saveNBT().toBuilder().putInt("countdown", 23).build());
        assertEquals(23, blockEntity.getCountdown());
    }

    @Test
    void firstTickSchedulesAnUpdateWhenNoneIsPending() {
        build(BlockTypes.STONE.getDefaultState(), 0);
        assertFalse(dimension.getBlockUpdateManager().hasScheduledBlockUpdate(origin));

        var component = (BlockEntityPotentSulfurBaseComponentImpl) ((BlockEntityImpl) blockEntity()).getBaseComponent();
        component.tick(0);

        assertTrue(dimension.getBlockUpdateManager().hasScheduledBlockUpdate(origin));
    }

    @Test
    void magmaGeyserEruptsAndSettlesOnRealTicks() throws InterruptedException {
        build(BlockTypes.MAGMA.getDefaultState(), 1);
        blockEntity().setCountdown(0);

        var loader = new FakeChunkLoader(() -> new Location3d(origin.x, origin.y, origin.z, dimension), 1);
        dimension.getChunkManager().addChunkLoader(loader);
        try {
            assertTrue(waitFor(() -> state() == PotentSulfurState.ERUPTING, 5000), "geyser hic patlamadi");
            var eruption = computeEruptionDurationSeconds(1, origin.x, origin.y, origin.z);
            assertTrue(waitFor(() -> state() == PotentSulfurState.DORMANT, (eruption + 3) * 1000L), "geyser patlamada kaldi");
            assertEquals(computeDormantDurationSeconds(1, origin.x, origin.y, origin.z), blockEntity().getCountdown(), 1);
        } finally {
            dimension.getChunkManager().removeChunkLoader(loader);
        }
    }

    @Test
    void geyserPushLiftsOnlyEntitiesInsideTheColumn() {
        var pos = new Vector3i(10, 64, 10);
        var mockDimension = mock(Dimension.class);
        when(mockDimension.getLiquid(anyInt(), anyInt(), anyInt())).thenReturn(new Pair<>(0, BlockTypes.WATER.getDefaultState()));

        var sinking = mock(EntityItem.class);
        stubEntity(sinking, mockDimension, 10.5, 65.2, 10.5, new Vector3d(0.1, -0.04, 0.2));
        var rising = mock(EntityItem.class);
        stubEntity(rising, mockDimension, 10.5, 65.6, 10.5, new Vector3d(0, 0.5, 0));
        var besideColumn = mock(EntityItem.class);
        stubEntity(besideColumn, mockDimension, 11.05, 65.2, 10.5, new Vector3d(0, -0.04, 0));
        var player = mock(EntityPlayer.class);
        stubEntity(player, mockDimension, 10.5, 65.0, 10.5, new Vector3d(0, -0.04, 0));
        var fallingBlock = mock(EntityFallingBlock.class);
        stubEntity(fallingBlock, mockDimension, 10.5, 65.4, 10.5, new Vector3d(0, -0.04, 0));
        stubWorld(mockDimension, List.of(sinking, rising, besideColumn, player, fallingBlock), 14, 70, 10);

        BlockPotentSulfurBaseComponentImpl.applyGeyserPush(mockDimension, pos, new BlockPotentSulfurBaseComponentImpl.WaterColumn(2, 66));

        var motion = ArgumentCaptor.forClass(Vector3dc.class);
        verify(sinking).resetFallDistance();
        verify(sinking).setMotion(motion.capture());
        assertEquals(0.1, motion.getValue().x(), 1e-9);
        assertEquals(0.08, motion.getValue().y(), 1e-9);
        assertEquals(0.2, motion.getValue().z(), 1e-9);

        verify(rising).resetFallDistance();
        verify(rising, never()).setMotion(any(Vector3dc.class));
        verify(besideColumn, never()).resetFallDistance();
        verify(player, never()).resetFallDistance();
        verify(player, never()).setMotion(any(Vector3dc.class));
        verify(fallingBlock, never()).resetFallDistance();
    }

    @Test
    void geyserPushNeedsAPlayerNearby() {
        var pos = new Vector3i(10, 64, 10);
        var mockDimension = mock(Dimension.class);
        when(mockDimension.getLiquid(anyInt(), anyInt(), anyInt())).thenReturn(new Pair<>(0, BlockTypes.WATER.getDefaultState()));
        var item = mock(EntityItem.class);
        stubEntity(item, mockDimension, 10.5, 65.2, 10.5, new Vector3d(0, -0.04, 0));
        stubWorld(mockDimension, List.of(item), 10, 64, 40);

        BlockPotentSulfurBaseComponentImpl.applyGeyserPush(mockDimension, pos, new BlockPotentSulfurBaseComponentImpl.WaterColumn(1, 65));

        verify(item, never()).resetFallDistance();
        verify(item, never()).setMotion(any(Vector3dc.class));
    }

    @Test
    void noxiousGasGivesNauseaToCreaturesOnTheSurface() {
        var pos = new Vector3i(10, 64, 10);
        var mockDimension = mock(Dimension.class);
        when(mockDimension.getLiquid(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                invocation.<Integer>getArgument(1) == 65 ? new Pair<>(0, BlockTypes.WATER.getDefaultState()) : Dimension.PAIR_LIQUID_NOT_FOUND);
        when(mockDimension.getBlockState(anyInt(), eq(66), anyInt())).thenReturn(BlockTypes.AIR.getDefaultState());

        var creature = mock(EntityLiving.class);
        stubEntity(creature, mockDimension, 12.5, 65.0, 10.5, new Vector3d());
        var item = mock(EntityItem.class);
        stubEntity(item, mockDimension, 12.5, 65.0, 10.5, new Vector3d());
        stubWorld(mockDimension, List.of(creature, item), 12, 65, 10);

        var column = new BlockPotentSulfurBaseComponentImpl.WaterColumn(1, 65);
        assertTrue(BlockPotentSulfurBaseComponentImpl.canEmitNoxiousGas(mockDimension, pos, column));
        BlockPotentSulfurBaseComponentImpl.applyNoxiousGas(mockDimension, pos, column);

        var effect = ArgumentCaptor.forClass(EffectInstance.class);
        verify(creature).addEffect(effect.capture());
        assertEquals(EffectTypes.NAUSEA, effect.getValue().getType());
        assertEquals(0, effect.getValue().getAmplifier());
        assertEquals(80, effect.getValue().getDuration());
        assertTrue(effect.getValue().isAmbient());
        assertFalse(effect.getValue().isVisible());
        verify(item, never()).addEffect(any());
    }

    @Test
    void coveredSurfaceDoesNotEmitGas() {
        var pos = new Vector3i(10, 64, 10);
        var mockDimension = mock(Dimension.class);
        when(mockDimension.getLiquid(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                invocation.<Integer>getArgument(1) == 65 ? new Pair<>(0, BlockTypes.WATER.getDefaultState()) : Dimension.PAIR_LIQUID_NOT_FOUND);
        when(mockDimension.getBlockState(anyInt(), eq(66), anyInt())).thenReturn(BlockTypes.STONE.getDefaultState());

        assertFalse(BlockPotentSulfurBaseComponentImpl.canEmitNoxiousGas(mockDimension, pos, new BlockPotentSulfurBaseComponentImpl.WaterColumn(1, 65)));
    }

    private void build(BlockState below, int waterHeight) {
        var x = origin.x;
        var y = origin.y;
        var z = origin.z;
        for (var dy = -1; dy <= 5; dy++) {
            dimension.setBlockState(x + 1, y + dy, z, BlockTypes.GLASS.getDefaultState());
            dimension.setBlockState(x - 1, y + dy, z, BlockTypes.GLASS.getDefaultState());
            dimension.setBlockState(x, y + dy, z + 1, BlockTypes.GLASS.getDefaultState());
            dimension.setBlockState(x, y + dy, z - 1, BlockTypes.GLASS.getDefaultState());
        }
        dimension.setBlockState(x, y - 2, z, BlockTypes.STONE.getDefaultState());
        dimension.setBlockState(x, y - 1, z, below);
        for (var dy = 1; dy <= waterHeight; dy++) {
            dimension.setBlockState(x, y + dy, z, BlockTypes.WATER.getDefaultState());
        }
        dimension.setBlockState(x, y, z, BlockTypes.POTENT_SULFUR.getDefaultState());
        settle();
    }

    private void settle() {
        var blockUpdateManager = (AllayBlockUpdateManager) dimension.getBlockUpdateManager();
        var done = new CompletableFuture<Void>();
        dimension.getScheduler().runLater(dimension, () -> {
            for (var i = 0; i < 4; i++) {
                blockUpdateManager.tick();
            }
            done.complete(null);
        });
        done.orTimeout(5, TimeUnit.SECONDS).join();
    }

    private PotentSulfurState state() {
        return dimension.getBlockState(origin).getPropertyValue(BlockPropertyTypes.POTENT_SULFUR_STATE);
    }

    private BlockEntityPotentSulfur blockEntity() {
        return BlockTypes.POTENT_SULFUR.getBlockBehavior().getBlockEntity(origin.x, origin.y, origin.z, dimension);
    }

    private void scheduledUpdate() {
        BlockTypes.POTENT_SULFUR.getBlockBehavior().onScheduledUpdate(new Block(dimension, origin));
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

    private static void stubEntity(Entity entity, Dimension dimension, double x, double y, double z, Vector3d motion) {
        when(entity.getLocation()).thenReturn(new Location3d(x, y, z, dimension));
        when(entity.getOffsetAABB()).thenReturn(new AABBd(x - 0.3, y, z - 0.3, x + 0.3, y + 0.25, z + 0.3));
        if (entity instanceof org.allaymc.api.entity.component.EntityPhysicsComponent physics) {
            when(physics.getMotion()).thenReturn(motion);
        }
    }

    @SuppressWarnings("unchecked")
    private static void stubWorld(Dimension dimension, List<Entity> entities, double playerX, double playerY, double playerZ) {
        var player = mock(Player.class);
        var playerEntity = mock(EntityPlayer.class);
        when(player.getControlledEntity()).thenReturn(playerEntity);
        when(playerEntity.getLocation()).thenReturn(new Location3d(playerX, playerY, playerZ, dimension));
        when(dimension.getPlayers()).thenReturn(Set.of(player));

        var physicsEngine = mock(EntityPhysicsEngine.class);
        when(physicsEngine.computeCollidingEntities(any(AABBdc.class), any(AABBOverlapFilter.class))).thenAnswer(invocation -> {
            AABBOverlapFilter<Entity> filter = invocation.getArgument(1);
            return entities.stream().filter(filter).toList();
        });
        var entityManager = mock(EntityManager.class);
        when(entityManager.getPhysicsService()).thenReturn(physicsEngine);
        when(dimension.getEntityManager()).thenReturn(entityManager);
    }
}
