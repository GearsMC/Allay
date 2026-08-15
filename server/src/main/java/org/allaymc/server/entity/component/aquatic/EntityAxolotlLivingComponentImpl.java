package org.allaymc.server.entity.component.aquatic;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.server.entity.component.EntityLivingComponentImpl;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

import java.util.List;

/**
 * Akselot icin canli varlik bileseni.
 *
 * <p>Akselot suda yasar ama balik degil: karada hemen olmez, bir sure dayanir ve ancak o sure
 * dolduktan sonra zarar gormeye baslar. Vanilla'daki bes dakikalik kuru kalma payi burada da
 * uygulaniyor.</p>
 *
 * <p>Akselot oyuncuya <strong>hicbir kosulda</strong> saldirmaz; vurulunca bile. Resmi davranis
 * paketinde {@code minecraft:behavior.hurt_by_target} bileseni hic yok ve hedef listesi yalnizca
 * suda bulunan murekkep baligi, balik, iribas ile bogulmus ve muhafiz ailelerinden olusuyor.
 * Vurulunca karsilik vermesi bu yuzden kaldirildi; onceki hali vanilla'da karsiligi olmayan bir
 * ekti.</p>
 */
public class EntityAxolotlLivingComponentImpl extends EntityLivingComponentImpl {

    /** Karada zarar gormeden dayanabildigi sure (tick); vanilla'da bes dakika. */
    protected static final int LAND_GRACE_TICKS = 20 * 60 * 5;

    /** Kuruduktan sonra her hasar arasindaki tick sayisi. */
    protected static final int LAND_DAMAGE_INTERVAL = 20;

    /** Karada her seferinde verilen hasar. */
    protected static final float LAND_DAMAGE = 1;

    protected int landTicks;
    protected int landDamageCooldown;

    public EntityAxolotlLivingComponentImpl() {
        setMaxHealth(14);
    }

    @Override
    public boolean canBreathe() {
        return true;
    }

    @Override
    public boolean hasDrowningDamage() {
        return false;
    }

    @Override
    public boolean hasFallDamage() {
        return false;
    }

    @EventHandler
    protected void onLandTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive() || isInWater()) {
            landTicks = 0;
            landDamageCooldown = 0;
            return;
        }

        if (++landTicks < LAND_GRACE_TICKS) {
            return;
        }

        if (landDamageCooldown > 0) {
            landDamageCooldown--;
            return;
        }

        landDamageCooldown = LAND_DAMAGE_INTERVAL;
        attack(DamageContainer.magicEffect(LAND_DAMAGE), true);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        return List.of();
    }

    @Override
    public int getDropXpAmount() {
        return 1;
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
