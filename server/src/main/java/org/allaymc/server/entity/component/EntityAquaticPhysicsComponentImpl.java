package org.allaymc.server.entity.component;

import org.allaymc.api.block.data.BlockTags;

/**
 * Suda yasayan moblar icin ortak fizik.
 *
 * <p>Su icinde varlik notr yuzerlikte olur — ne batar ne de yuzeye itilir — cunku nerede duracagina
 * yuzme kontrolcusu karar veriyor; kaldirma kuvveti onu surekli yuzeye tirmandirsaydi hicbir mob
 * dipte kalamazdi. Resmi tanimdaki {@code "can_sink": false} de bunu soyluyor.</p>
 *
 * <p>Sudan cikinca yercekimi geri geliyor. Bu hali akselota uyuyor: o amfibi, karada yuruyor ve
 * resmi tanimda yercekimi acik. Balik icin gecerli degil; bkz.
 * {@link org.allaymc.server.entity.component.aquatic.EntityFishPhysicsComponentImpl}.</p>
 */
public class EntityAquaticPhysicsComponentImpl extends EntityPhysicsComponentImpl {

    @Override
    public double getGravity() {
        // Suyun icinde agirlik yok; disarida normal dususe geri don.
        return isInWater() ? 0 : super.getGravity();
    }

    @Override
    public double getWaterBuoyancy() {
        // Notr yuzerlik: kaldirma yok, cunku dikey konumu yuzme kontrolcusu belirliyor.
        return 0;
    }

    @Override
    public double getWaterDragFactor() {
        // Suyun direnci; kontrolcu itmeyi kestiginde varlik cabucak duruyor.
        return 0.1;
    }

    @Override
    public double getStepHeight() {
        // Karaya vurdugunda cirpinabilmesi icin kucuk bir adim payi.
        return 0.4;
    }

    protected boolean isInWater() {
        var loc = thisEntity.getLocation();
        var blockState = thisEntity.getDimension().getBlockState(
                (int) Math.floor(loc.x()),
                (int) Math.floor(loc.y()),
                (int) Math.floor(loc.z())
        );
        return blockState.getBlockType().hasBlockTag(BlockTags.WATER);
    }
}
