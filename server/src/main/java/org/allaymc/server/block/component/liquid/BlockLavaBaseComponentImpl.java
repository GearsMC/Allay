package org.allaymc.server.block.component.liquid;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.eventbus.event.block.BlockIgniteEvent;
import org.allaymc.api.eventbus.event.block.LiquidHardenEvent;
import org.allaymc.api.eventbus.event.entity.EntityCombustEvent;
import org.allaymc.api.eventbus.event.entity.EntityDamageEvent;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.math.position.Position3i;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.biome.BiomeType;
import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.dimension.DimensionType;
import org.allaymc.api.world.gamerule.GameRule;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.concurrent.ThreadLocalRandom;

import static org.allaymc.api.block.component.BlockLiquidBaseComponent.getDepth;
import static org.allaymc.api.block.component.BlockLiquidBaseComponent.isFalling;
import static org.allaymc.api.block.component.BlockLiquidBaseComponent.isSource;

/**
 * @author daoge_cmd
 */
public class BlockLavaBaseComponentImpl extends BlockLiquidBaseComponentImpl {
    public BlockLavaBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
    }

    @Override
    public boolean isSameLiquidType(BlockType<?> blockType) {
        return blockType.hasBlockTag(BlockTags.LAVA);
    }

    @Override
    public void onEntityInside(Block block, Entity entity) {
        if (!(entity instanceof EntityLiving living)) {
            return;
        }

        // Set on fire ticks
        var event1 = new EntityCombustEvent(entity, EntityCombustEvent.CombusterType.BLOCK, block, 20 * 15);
        if (event1.call()) {
            living.setOnFireTicks(event1.getOnFireTicks());
        }

        // Lava damage
        if (living.hasFireDamage() && entity.getTick() % 10 == 0) {
            var event2 = new EntityDamageEvent(entity, DamageContainer.lava(4));
            if (event2.call()) {
                living.attack(event2.getDamageContainer(), true);
            }
        }
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face, BlockState oldNeighborState) {
        if (!tryHarden(block, null)) {
            super.onNeighborUpdate(block, neighbor, face, oldNeighborState);
        }
    }

    @Override
    public void onScheduledUpdate(Block block) {
        super.onScheduledUpdate(block);
        var current = new Block(block.getDimension(), block.getPosition());
        if (current.getBlockType() == block.getBlockType()) {
            tryHarden(current, null);
        }
    }

    @Override
    public void afterPlaced(Block oldBlock, BlockState newBlockState, PlayerInteractInfo placementInfo) {
        super.afterPlaced(oldBlock, newBlockState, placementInfo);
        tryHarden(new Block(newBlockState, oldBlock.getPosition(), oldBlock.getLayer()), null);
    }

    /**
     * SkyBuild {@code Lava::checkForHarden} karsiligi. Vanilla'daki kaynak lavadan obsidyen kurali
     * yok; sertlesen blok her zaman lavanin kendisidir, komsu blok yerinde kalir.
     *
     * <ul>
     *   <li>Yan/ust komsu su: kaynak lava hicbir sey yapmaz; akan lava (PM decay &lt;= 4) kendisi
     *       cobblestone'a doner (binde 1 redstone, binde 2 lapis cevheri).</li>
     *   <li>Yan/ust komsu paketli buz: lava tasa doner (buz yerinde kalir).</li>
     *   <li>Cehennem biyomunda, lavanin ustunde veya altinda ruh topragi varsa: yanindaki yaldizli
     *       blackstone lavayi blackstone/netherrack/cevhere, mavi buz bazalt/derin arduvaz cevherine
     *       cevirir.</li>
     * </ul>
     *
     * <p>PM'de su lavanin icine akmaz; yalnizca lava suya akinca su tasa doner (bkz. su tarafi).</p>
     */
    @Override
    public boolean tryHarden(Block block, Block flownIntoBy) {
        if (flownIntoBy != null) {
            return false;
        }
        var state = block.getBlockState();
        if (isFalling(state)) {
            return false;
        }

        var dimension = block.getDimension();
        var pos = block.getPosition();
        // PM decay: kaynak 0, akan sivi 8 - derinlik.
        var decay = isSource(state) ? 0 : 8 - getDepth(state);
        var hellBiome = isHellBiome(dimension.getBiome(pos));
        var soulSoil = hellBiome
                && (block.offsetPos(BlockFace.UP).getBlockType() == BlockTypes.SOUL_SOIL
                || block.offsetPos(BlockFace.DOWN).getBlockType() == BlockTypes.SOUL_SOIL);
        var random = ThreadLocalRandom.current();

        for (var face : BlockFace.VALUES) {
            if (face == BlockFace.DOWN) {
                continue;
            }
            var neighbor = block.offsetPos(face);
            var type = neighbor.getBlockType();

            if (BlockTypes.WATER.getBlockBehavior().isSameLiquidType(type)) {
                if (decay == 0) {
                    return true;
                }
                if (decay <= 4) {
                    var roll = random.nextInt(1, 1001);
                    if (roll <= 1) {
                        collide(block, neighbor, BlockTypes.REDSTONE_ORE.getDefaultState());
                    } else if (roll <= 3) {
                        collide(block, neighbor, BlockTypes.LAPIS_ORE.getDefaultState());
                    } else {
                        collide(block, neighbor, BlockTypes.COBBLESTONE.getDefaultState());
                    }
                    return true;
                }
            }

            if (type == BlockTypes.PACKED_ICE) {
                collide(block, neighbor, BlockTypes.STONE.getDefaultState());
                spawnParticles(block, SimpleParticle.SMOKE, 4);
                return true;
            }

            if (hellBiome && soulSoil) {
                if (type == BlockTypes.GILDED_BLACKSTONE) {
                    var roll = random.nextInt(1, 1001);
                    if (roll <= 5) {
                        collide(block, neighbor, BlockTypes.QUARTZ_ORE.getDefaultState());
                    } else if (roll <= 10) {
                        collide(block, neighbor, BlockTypes.NETHER_GOLD_ORE.getDefaultState());
                    } else if (roll <= 40) {
                        collide(block, neighbor, BlockTypes.NETHERRACK.getDefaultState());
                    } else {
                        collide(block, neighbor, BlockTypes.BLACKSTONE.getDefaultState());
                    }
                    return true;
                }
                if (type == BlockTypes.BLUE_ICE) {
                    var roll = random.nextInt(1, 1001);
                    if (roll <= 2) {
                        collide(block, neighbor, BlockTypes.DEEPSLATE_LAPIS_ORE.getDefaultState());
                    } else if (roll <= 4) {
                        collide(block, neighbor, BlockTypes.DEEPSLATE_GOLD_ORE.getDefaultState());
                    } else if (roll <= 18) {
                        collide(block, neighbor, BlockTypes.DEEPSLATE_COAL_ORE.getDefaultState());
                    } else if (roll <= 22) {
                        collide(block, neighbor, BlockTypes.DEEPSLATE_IRON_ORE.getDefaultState());
                    } else {
                        collide(block, neighbor, BlockTypes.BASALT.getDefaultState());
                    }
                    spawnParticles(block, SimpleParticle.LAVA, 6);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * SkyBuild {@code Liquid::liquidCollide}: sonuc {@code $this}, yani LAVANIN KENDISI olur
     * ({@code BlockEventHelper::form($this, $result, $cause)}); komşu su/buz/blackstone yerinde
     * kalir. Boylece jenerator kendi suyunu yemez, lav sertlesip akisi durdurur.
     */
    private void collide(Block lava, Block cause, BlockState result) {
        var event = new LiquidHardenEvent(lava, cause.getBlockState(), result, lava.getPosition());
        if (!event.call()) {
            return;
        }
        var dimension = lava.getDimension();
        dimension.setBlockState(lava.getPosition(), event.getHardenedBlockState());
        dimension.addSound(MathUtils.center(lava.getPosition()), SimpleSound.FIZZ);
    }

    private static void spawnParticles(Block lava, SimpleParticle particle, int count) {
        var dimension = lava.getDimension();
        var pos = lava.getPosition();
        var random = ThreadLocalRandom.current();
        for (var i = 0; i < count; i++) {
            dimension.addParticle(
                    pos.x() + random.nextInt(-10, 11) / 10.0,
                    pos.y() + random.nextInt(0, 11) / 10.0,
                    pos.z() + random.nextInt(-10, 11) / 10.0,
                    particle);
        }
    }

    private static boolean isHellBiome(BiomeType biome) {
        return biome == BiomeTypes.HELL
                || biome == BiomeTypes.CRIMSON_FOREST
                || biome == BiomeTypes.WARPED_FOREST
                || biome == BiomeTypes.BASALT_DELTAS
                || biome == BiomeTypes.SOULSAND_VALLEY;
    }

    // See https://minecraft.wiki/w/Lava#Fire_spread
    @Override
    public void onRandomUpdate(Block block) {
        if (!block.getDimension().getWorld().getWorldData().<Boolean>getGameRuleValue(GameRule.DO_FIRE_TICK)) {
            return;
        }

        var pos = block.getPosition();
        var dimension = block.getDimension();
        var random = ThreadLocalRandom.current();
        var i = random.nextInt(3);

        if (i > 0) {
            for (int k = 0; k < i; ++k) {
                var v = new Vector3i(pos.x() + random.nextInt(3) - 2, pos.y() + 1, pos.z() + random.nextInt(3) - 2);
                var blockState = dimension.getBlockState(v);

                if (blockState.getBlockType() == BlockTypes.AIR) {
                    if (!this.canNeighborBurn(dimension, v)) {
                        continue;
                    }

                    var event = new BlockIgniteEvent(new Block(blockState, new Position3i(v, dimension), 0), block, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                    if (event.call()) {
                        var fireBlockState = getFireBlockState(dimension, v);
                        dimension.setBlockState(v, fireBlockState);
                    }

                    return;
                }
            }
        } else {
            for (int k = 0; k < 3; ++k) {
                var v = new Vector3i(pos.x() + random.nextInt(3) - 1, pos.y(), pos.z() + random.nextInt(3) - 1);
                var blockState = dimension.getBlockState(v);
                if (dimension.getBlockState(v.x(), v.y() + 1, v.z()).getBlockType() != BlockTypes.AIR || blockState.getBlockStateData().flameOdds() <= 0) {
                    continue;
                }

                var event = new BlockIgniteEvent(new Block(blockState, new Position3i(v, dimension), 0), block, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                if (event.call()) {
                    var fireBlockState = getFireBlockState(dimension, v);
                    dimension.setBlockState(v, fireBlockState);
                }
            }
        }
    }

    protected BlockState getFireBlockState(Dimension dimension, Vector3ic pos) {
        // Check if the block that the player clicked on is a soul fire converter
        // In that case, we should place a soul fire instead of a normal fire
        return dimension.getBlockState(BlockFace.DOWN.offsetPos(pos)).getBlockType().hasBlockTag(BlockTags.SOUL_FIRE_CONVERTER) ? BlockTypes.SOUL_FIRE.getDefaultState() : BlockTypes.FIRE.getDefaultState();
    }

    protected boolean canNeighborBurn(Dimension dimension, Vector3ic pos) {
        for (var face : BlockFace.VALUES) {
            if (dimension.getBlockState(face.offsetPos(pos)).getBlockStateData().flameOdds() > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canRandomUpdate() {
        return true;
    }

    @Override
    public int getFlowDecay(DimensionType dimensionType) {
        // SkyBuild Lava::getFlowDecayPerBlock: her boyutta 2.
        return 2;
    }

    @Override
    public int getFlowSpeed(DimensionType dimensionType) {
        // SkyBuild Lava::tickRate: her boyutta 30 tik (nether'de hizlanmaz).
        return 30;
    }

    @Override
    public boolean canFormSource() {
        return false;
    }

    @Override
    public boolean canBeContained() {
        return false;
    }
}
