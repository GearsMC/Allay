package org.allaymc.server.entity.component.aquatic;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.server.entity.component.EntityLivingComponentImpl;
import org.allaymc.server.entity.component.event.CEntityTickEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Baliklarin paylastigi canli varlik bileseni.
 *
 * <p>Balik icin bogulma tersine isliyor: suda hicbir sorunu yok, karada bogulur. Motorun
 * bogulma mantigi kara varliklari icin yazildigindan burada kapatiliyor ve yerine karada
 * gecirilen sureyi sayan kendi hasar sayaci konuyor.</p>
 */
public class EntityFishLivingComponentImpl extends EntityLivingComponentImpl {

    /** Karada her hasar arasindaki tick sayisi. */
    protected static final int LAND_DAMAGE_INTERVAL = 20;

    /** Karada her seferinde verilen hasar. */
    protected static final float LAND_DAMAGE = 1;

    protected final Supplier<ItemType<?>> dropSupplier;
    protected int landDamageCooldown;

    /**
     * @param maxHealth baligin cani
     * @param dropSupplier oldugunde dusurdugu esya turunu saglar; gec cozulur cunku
     *                     {@code ItemTypes} alanlari turler kaydedilirken hala null'dur
     */
    public EntityFishLivingComponentImpl(float maxHealth, Supplier<ItemType<?>> dropSupplier) {
        this.dropSupplier = dropSupplier;
        setMaxHealth(maxHealth);
    }

    @Override
    public boolean canBreathe() {
        // Balik suda nefes alir; motorun "gozler suyun altinda" kurali tam tersini soyluyor.
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
            landDamageCooldown = 0;
            return;
        }

        if (landDamageCooldown > 0) {
            landDamageCooldown--;
            return;
        }

        landDamageCooldown = LAND_DAMAGE_INTERVAL;
        // Vurulma sonrasi dokunulmazlik penceresi yok sayiliyor; karada kalmak duzenli araliklarla
        // isleyen bir hasar, tek seferlik bir vurus degil.
        attack(DamageContainer.magicEffect(LAND_DAMAGE), true);
    }

    @Override
    public List<ItemStack> getDrops(int lootingLevel) {
        var drop = dropSupplier.get();
        if (drop == null) {
            return List.of();
        }

        return List.of(drop.createItemStack());
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
