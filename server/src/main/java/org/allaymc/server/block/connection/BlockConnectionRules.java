package org.allaymc.server.block.connection;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.interfaces.*;
import org.allaymc.api.block.property.enums.MinecraftCorner;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.property.type.BooleanPropertyType;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;

import static org.allaymc.server.block.BlockPlaceHelper.EWSN_DIRECTION_4_MAPPER;

/**
 * 26.50 bağlantı ({@code minecraft:connection_*}) ve köşe ({@code minecraft:corner}) durumlarının vanilla kuralları.
 *
 * <p>GearsMC fork (yol haritası Adım 6.8). Saf hesap: dünyaya dokunmaz, komşuları {@link NeighborReader}'dan okur. Oyun
 * içi bileşenler ve dünya dönüştürme aracı aynı kuralı kullanır. Doğruluk kaynağı vanilla kahinin altın tablosudur
 * ({@code server/src/test/resources/vanilla-oracle}); kurallar {@code BlockConnectionRulesTest} ile o tablodan sınanır.
 * Ölçüm vanillanın Java kurallarını izlediğini gösterdi, PocketMine'dan şu noktalarda ayrılır:</p>
 * <ul>
 *     <li>S1: çit, çit kapısına kapı ekseni bağlantı yönüne dikse bağlanır (PM ekseni ters kontrol ediyordu).</li>
 *     <li>S2: nether tuğlası çiti yalnızca nether tuğlası çitine bağlanır, tahta çitler birbirine (PM hepsini bağlıyordu).</li>
 *     <li>S6: bağlanılan yüz ölçülmüş tablodan gelir ({@link BlockConnectionFaces}); PM yaprak, balkabağı, karpuz ve
 *     shulker kutusunu ayrıca istisna tutuyordu.</li>
 *     <li>S9: tuzak ipi yalnızca ipe ve kendisine bakan kancaya bağlanır.</li>
 *     <li>S10: merdiven köşesinde Java'nın "şekil alabilir" koşulu uygulanır (PM bu engeli yapmıyordu).</li>
 * </ul>
 */
public final class BlockConnectionRules {

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private BlockConnectionRules() {
    }

    /** Aynı yükseklikteki yatay komşuyu verir. */
    @FunctionalInterface
    public interface NeighborReader {
        BlockState get(BlockFace face);
    }

    /**
     * Bloğun bağlantı ya da köşe durumunu komşularına göre yeniden hesaplar.
     *
     * @return güncel durum; kural dışındaki blokta {@code state} aynen döner
     */
    public static BlockState update(BlockState state, NeighborReader neighbors) {
        var behavior = state.getBehavior();
        if (behavior instanceof BlockFenceBehavior) {
            return withConnections(state, face -> fenceConnectsTo(state, neighbors.get(face), face));
        }
        if (isPaneLike(behavior)) {
            return withConnections(state, face -> paneConnectsTo(neighbors.get(face), face));
        }
        if (behavior instanceof BlockTripWireBehavior) {
            return withConnections(state, face -> tripWireConnectsTo(neighbors.get(face), face));
        }
        if (isStairs(state)) {
            return state.setPropertyValue(BlockPropertyTypes.MINECRAFT_CORNER, stairsCorner(state, neighbors));
        }
        return state;
    }

    /** Durumu bu sınıfın hesapladığı bir blok mu. */
    public static boolean isConnectionBlock(BlockState state) {
        var behavior = state.getBehavior();
        return behavior instanceof BlockFenceBehavior || isPaneLike(behavior)
               || behavior instanceof BlockTripWireBehavior || isStairs(state);
    }

    static boolean fenceConnectsTo(BlockState fence, BlockState neighbor, BlockFace face) {
        var behavior = neighbor.getBehavior();
        if (behavior instanceof BlockFenceBehavior) {
            return isNetherBrickFence(fence) == isNetherBrickFence(neighbor);
        }
        if (behavior instanceof BlockFenceGateBehavior) {
            var gateFacing = BlockFace.from(neighbor.getPropertyValue(BlockPropertyTypes.MINECRAFT_CARDINAL_DIRECTION));
            return gateFacing.getAxis() != face.getAxis();
        }
        return BlockConnectionFaces.isConnectable(neighbor, face.opposite());
    }

    static boolean paneConnectsTo(BlockState neighbor, BlockFace face) {
        var behavior = neighbor.getBehavior();
        if (isPaneLike(behavior) || behavior instanceof BlockWallBehavior || neighbor.getBlockType() == BlockTypes.BORDER_BLOCK) {
            return true;
        }
        return BlockConnectionFaces.isConnectable(neighbor, face.opposite());
    }

    static boolean tripWireConnectsTo(BlockState neighbor, BlockFace face) {
        if (neighbor.getBehavior() instanceof BlockTripWireBehavior) {
            return true;
        }
        if (neighbor.getBehavior() instanceof BlockTripwireHookBehavior) {
            // Kancanın yönü (direction) baktığı yandır; ipe doğru bakan kanca bağlanır.
            var hookFacing = BlockFace.fromHorizontalIndex(neighbor.getPropertyValue(BlockPropertyTypes.DIRECTION_4));
            return hookFacing == face.opposite();
        }
        return false;
    }

    /**
     * Java {@code StairBlock#getStairsShape}: önce ön komşu dış köşe, sonra arka komşu iç köşe verir. Merdivenin yönü
     * ({@code weirdo_direction}) yüksek tarafın baktığı yandır; sol/sağ adları vanilla ile aynı (S10 altın tablosu).
     */
    static MinecraftCorner stairsCorner(BlockState stairs, NeighborReader neighbors) {
        var facing = stairsFacing(stairs);
        var front = neighbors.get(facing);
        if (isStairs(front) && sameHalf(stairs, front)) {
            var frontFacing = stairsFacing(front);
            if (frontFacing.getAxis() != facing.getAxis() && canTakeShape(stairs, neighbors, frontFacing.opposite())) {
                return frontFacing == facing.rotateYCCW() ? MinecraftCorner.OUTER_LEFT : MinecraftCorner.OUTER_RIGHT;
            }
        }
        var back = neighbors.get(facing.opposite());
        if (isStairs(back) && sameHalf(stairs, back)) {
            var backFacing = stairsFacing(back);
            if (backFacing.getAxis() != facing.getAxis() && canTakeShape(stairs, neighbors, backFacing)) {
                return backFacing == facing.rotateYCCW() ? MinecraftCorner.INNER_LEFT : MinecraftCorner.INNER_RIGHT;
            }
        }
        return MinecraftCorner.NONE;
    }

    private static boolean canTakeShape(BlockState stairs, NeighborReader neighbors, BlockFace face) {
        var neighbor = neighbors.get(face);
        return !isStairs(neighbor) || stairsFacing(neighbor) != stairsFacing(stairs) || !sameHalf(stairs, neighbor);
    }

    private static BlockFace stairsFacing(BlockState stairs) {
        return EWSN_DIRECTION_4_MAPPER.inverse().get(stairs.getPropertyValue(BlockPropertyTypes.WEIRDO_DIRECTION));
    }

    private static boolean sameHalf(BlockState a, BlockState b) {
        return a.getPropertyValue(BlockPropertyTypes.UPSIDE_DOWN_BIT) == b.getPropertyValue(BlockPropertyTypes.UPSIDE_DOWN_BIT);
    }

    // Köşe durumu yalnızca merdivenlerde var; bakır merdivenin davranış arayüzü ayrı olduğu için türe değil duruma bakılır.
    private static boolean isStairs(BlockState state) {
        return state.getBlockType().hasProperty(BlockPropertyTypes.MINECRAFT_CORNER);
    }

    private static boolean isPaneLike(Object behavior) {
        return behavior instanceof BlockGlassPaneBehavior || behavior instanceof BlockIronBarsBehavior
               || behavior instanceof BlockCopperBarsBehavior;
    }

    private static boolean isNetherBrickFence(BlockState state) {
        return state.getBlockType() == BlockTypes.NETHER_BRICK_FENCE;
    }

    private interface FaceTest {
        boolean connects(BlockFace face);
    }

    private static BlockState withConnections(BlockState state, FaceTest test) {
        for (var face : HORIZONTAL) {
            state = state.setPropertyValue(connectionProperty(face), test.connects(face));
        }
        return state;
    }

    private static BooleanPropertyType connectionProperty(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockPropertyTypes.MINECRAFT_CONNECTION_NORTH;
            case EAST -> BlockPropertyTypes.MINECRAFT_CONNECTION_EAST;
            case SOUTH -> BlockPropertyTypes.MINECRAFT_CONNECTION_SOUTH;
            case WEST -> BlockPropertyTypes.MINECRAFT_CONNECTION_WEST;
            default -> throw new IllegalArgumentException("Yatay olmayan yüz: " + face);
        };
    }
}
