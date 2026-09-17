package org.allaymc.server.block.connection;

import org.allaymc.api.block.property.enums.MinecraftCorner;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bağlantı bileşenlerini gerçek test dünyasında sınar (yol haritası Adım 6.11): koy/kır sonrası komşular güncelleniyor
 * ve güncelleme zinciri duruyor. Komşu güncellemeleri dünyanın kendi tick'inde işlendiği için sonuç beklenerek okunur.
 */
@ExtendWith(AllayTestExtension.class)
class BlockConnectionWorldTest {

    private static final long TIMEOUT_MILLIS = 5000;
    private static final long QUIET_MILLIS = 500;

    private Dimension dimension;
    private Vector3i origin;

    @BeforeEach
    void setUp() {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        dimension = spawn.dimension();
        // Doğuş noktasının üstünde, havada: zemin blokları bağlantıyı etkilemesin.
        origin = new Vector3i((int) spawn.x() + 64, (int) spawn.y() + 40, (int) spawn.z() + 64);
        for (var dx = -3; dx <= 3; dx++) {
            for (var dz = -3; dz <= 3; dz++) {
                dimension.getChunkManager().getOrLoadChunk((origin.x + dx) >> 4, (origin.z + dz) >> 4).join();
                dimension.setBlockState(origin.x + dx, origin.y, origin.z + dz, BlockTypes.AIR.getDefaultState());
            }
        }
    }

    @Test
    void fenceLineConnectsAndDisconnectsWhenMiddleIsBroken() {
        var west = at(-1, 0);
        var middle = at(0, 0);
        var east = at(1, 0);
        for (var pos : new Vector3ic[]{west, middle, east}) {
            assertTrue(dimension.setBlockState(pos, BlockTypes.OAK_FENCE.getDefaultState()));
        }

        awaitTrue(() -> connected(west, BlockPropertyTypes.MINECRAFT_CONNECTION_EAST)
                        && connected(middle, BlockPropertyTypes.MINECRAFT_CONNECTION_WEST)
                        && connected(middle, BlockPropertyTypes.MINECRAFT_CONNECTION_EAST)
                        && connected(east, BlockPropertyTypes.MINECRAFT_CONNECTION_WEST),
                "çit dizisi bağlanmadı");
        assertFalse(connected(west, BlockPropertyTypes.MINECRAFT_CONNECTION_WEST));
        assertQuiet(west, middle, east);

        dimension.setBlockState(middle, BlockTypes.AIR.getDefaultState());
        awaitTrue(() -> !connected(west, BlockPropertyTypes.MINECRAFT_CONNECTION_EAST)
                        && !connected(east, BlockPropertyTypes.MINECRAFT_CONNECTION_WEST),
                "orta çit kırılınca uçlar ayrılmadı");
        assertQuiet(west, east);
    }

    @Test
    void placeComputesConnectionsBeforeTheBlockIsWritten() {
        var stone = at(0, -1);
        var pane = at(0, 0);
        dimension.setBlockState(stone, BlockTypes.STONE.getDefaultState());

        var state = BlockTypes.GLASS_PANE.getDefaultState();
        assertTrue(state.getBehavior().place(dimension, state, pane, null));

        // Komşu güncellemesi beklenmeden: yerleştirme bağlantıyı yazmadan önce hesaplıyor.
        assertTrue(connected(pane, BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH));
        assertFalse(connected(pane, BlockPropertyTypes.MINECRAFT_CONNECTION_SOUTH));
    }

    @Test
    void stairsFormOuterCornerAndStraightenWhenNeighbourLeaves() {
        // Altın tablo STAIRS/w0/bottom/east/w3: doğuya bakan merdivenin önünde kuzeye bakan merdiven → outer_left.
        var stairs = at(0, 0);
        var front = at(1, 0);
        dimension.setBlockState(stairs, stairs(0));
        dimension.setBlockState(front, stairs(3));

        awaitTrue(() -> corner(stairs) == MinecraftCorner.OUTER_LEFT, "dış köşe oluşmadı");
        assertQuiet(stairs, front);

        dimension.setBlockState(front, BlockTypes.AIR.getDefaultState());
        awaitTrue(() -> corner(stairs) == MinecraftCorner.NONE, "komşu kalkınca merdiven düzelmedi");
    }

    /**
     * GearsCore çekici bölgeyi tek şablon durumla {@code setBlockState} üzerinden doldurur (yol haritası 7.3). Şablon başka
     * yerden kopyalanmış bayat bağlantı/köşe taşısa da blok yerleşince komşulara göre düzelmeli.
     */
    @Test
    void bulkFillWithStaleStatesSettlesToNeighbours() {
        var staleFence = BlockTypes.OAK_FENCE.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH, true)
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_EAST, true)
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_SOUTH, true)
                .setPropertyValue(BlockPropertyTypes.MINECRAFT_CONNECTION_WEST, true);
        var row = new Vector3i[5];
        for (var i = 0; i < row.length; i++) {
            row[i] = at(i - 2, 2);
            dimension.setBlockState(row[i], staleFence);
        }
        var staleStairs = stairs(0).setPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER, MinecraftCorner.INNER_RIGHT);
        var lonelyStairs = at(0, -2);
        dimension.setBlockState(lonelyStairs, staleStairs);

        awaitTrue(() -> {
            for (var i = 0; i < row.length; i++) {
                if (connected(row[i], BlockPropertyTypes.MINECRAFT_CONNECTION_WEST) != (i > 0)
                    || connected(row[i], BlockPropertyTypes.MINECRAFT_CONNECTION_EAST) != (i < row.length - 1)
                    || connected(row[i], BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH)
                    || connected(row[i], BlockPropertyTypes.MINECRAFT_CONNECTION_SOUTH)) {
                    return false;
                }
            }
            return corner(lonelyStairs) == MinecraftCorner.NONE;
        }, "bayat durumla doldurulan bloklar düzelmedi");
        assertQuiet(row);
    }

    @Test
    void netherBrickFenceIgnoresWoodenFence() {
        var nether = at(0, 0);
        var oak = at(1, 0);
        dimension.setBlockState(nether, BlockTypes.NETHER_BRICK_FENCE.getDefaultState());
        dimension.setBlockState(oak, BlockTypes.OAK_FENCE.getDefaultState());
        assertQuiet(nether, oak);
        assertFalse(connected(nether, BlockPropertyTypes.MINECRAFT_CONNECTION_EAST));
        assertFalse(connected(oak, BlockPropertyTypes.MINECRAFT_CONNECTION_WEST));
    }

    private Vector3i at(int dx, int dz) {
        return new Vector3i(origin.x + dx, origin.y, origin.z + dz);
    }

    private static BlockState stairs(int direction) {
        return BlockTypes.OAK_STAIRS.getDefaultState().setPropertyValue(BlockPropertyTypes.WEIRDO_DIRECTION, direction);
    }

    private boolean connected(Vector3ic pos, org.allaymc.api.block.property.type.BooleanPropertyType property) {
        return dimension.getBlockState(pos).getPropertyValue(property);
    }

    private MinecraftCorner corner(Vector3ic pos) {
        return dimension.getBlockState(pos).getPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER);
    }

    private static void awaitTrue(BooleanSupplier condition, String message) {
        var deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail(message);
            }
            Thread.onSpinWait();
        }
    }

    /** Birkaç tick boyunca durumların değişmediğini doğrular: güncelleme zinciri kendi kendini beslemiyor. */
    private void assertQuiet(Vector3ic... positions) {
        sleep(QUIET_MILLIS);
        var before = new BlockState[positions.length];
        for (var i = 0; i < positions.length; i++) {
            before[i] = dimension.getBlockState(positions[i]);
        }
        sleep(QUIET_MILLIS);
        for (var i = 0; i < positions.length; i++) {
            assertEquals(before[i], dimension.getBlockState(positions[i]), "durum durulmadı: " + positions[i]);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
