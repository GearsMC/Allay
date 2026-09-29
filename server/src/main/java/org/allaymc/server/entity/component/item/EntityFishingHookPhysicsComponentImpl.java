package org.allaymc.server.entity.component.item;

import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityFishingHookBaseComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityFishingHook;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.math.location.Location3dc;
import org.allaymc.server.component.annotation.ComponentObject;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.component.projectile.EntityProjectilePhysicsComponentImpl;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Physics component for fishing hook entity.
 * Handles water floating and collision detection.
 *
 * @author daoge_cmd
 */
public class EntityFishingHookPhysicsComponentImpl extends EntityProjectilePhysicsComponentImpl {

    /**
     * GearsMC fork: PM kancasinin havadaki surtunme carpani ({@code motion * 0.95}).
     */
    protected static final double PM_AIR_DRAG_MULTIPLIER = 0.95;
    /**
     * GearsMC fork: PM kancasinin tik basina dusus payi. PM'de yercekimi 0.05 idi ama
     * kanca tikte iki kez hareket ettigi icin tek hareketli karsiligi iki katidir
     * (bkz. {@code ItemFishingRodBaseComponentImpl.THROW_FORCE}).
     */
    protected static final double PM_AIR_GRAVITY_PER_TICK = 0.1;

    @ComponentObject
    protected EntityFishingHook thisEntity;

    @Dependency
    protected EntityFishingHookBaseComponent fishingHookBaseComponent;

    /**
     * GearsMC fork: kanca yalnizca baska oyunculara takilir — PM {@code FishingHook::canCollideWith}.
     *
     * <p>Vanilla kanca carptigi her fizik varligina takiliyordu: yerdeki esyalara,
     * baska kancalara, yakalama gosterim esyasina ve 10 tikten sonra oltayi atan
     * oyuncunun kendisine. Kendine takilan kanca cekilince oyuncuyu kendine dogru
     * "cekiyordu". PM'de atan oyuncu hic carpismaz, yerde duran kanca kimseye
     * carpmaz ve oyuncu disindaki varliklarla cekme hic yoktu.</p>
     */
    @Override
    protected boolean shouldSkipEntityCollision(Entity entity) {
        if (onGround || !(entity instanceof EntityPlayer)) {
            return true;
        }
        return entity == projectileComponent.getShooter();
    }

    @Override
    protected void onHitEntity(Entity entity, Vector3dc hitPos) {
        if (fishingHookBaseComponent.hasHookedEntity()) {
            return;
        }

        // Hook the entity
        fishingHookBaseComponent.setHookedEntity(entity);
        // Deal damage to trigger hurt animation and knockback.
        // GearsMC fork: miktar eklentiden gelir; vanilla degeri sifirdir.
        if (entity instanceof EntityLiving living) {
            var damageContainer = DamageContainer.projectile(
                    thisEntity, fishingHookBaseComponent.getHookDamage());
            living.attack(damageContainer);
        }
    }

    @Override
    public Vector3d updateMotion(LiquidState liquidState) {
        // If hooked an entity, the hook will be teleported to the entity every tick,
        // so we do not need to update the motion
        if (fishingHookBaseComponent.hasHookedEntity()) {
            return new Vector3d(0, 0, 0);
        }

        if (liquidState.inWater()) {
            // Water physics: float and slow down
            var location = thisEntity.getLocation();
            double waterY = getWaterSurfaceY(location);
            if (location.y() < waterY - 0.1) {
                // Below water surface: float up
                return new Vector3d(
                        motion.x * 0.3,
                        0.15,
                        motion.z * 0.3
                );
            } else {
                // At water surface: stay still with minor drift
                return new Vector3d(
                        motion.x * 0.9,
                        motion.y * 0.3,
                        motion.z * 0.9
                );
            }
        } else {
            // Air physics: normal gravity
            // GearsMC fork: PM kancasinin hava fizigi (bkz. PM_AIR_GRAVITY_PER_TICK).
            return new Vector3d(
                    motion.x * PM_AIR_DRAG_MULTIPLIER,
                    motion.y * PM_AIR_DRAG_MULTIPLIER - PM_AIR_GRAVITY_PER_TICK,
                    motion.z * PM_AIR_DRAG_MULTIPLIER
            );
        }
    }

    protected double getWaterSurfaceY(Location3dc location) {
        var dimension = location.dimension();
        int x = (int) Math.floor(location.x());
        int z = (int) Math.floor(location.z());
        int y = (int) Math.floor(location.y());

        // Find the top of the water column
        while (y < dimension.getDimensionType().getMaxHeight()) {
            var blockState = dimension.getBlockState(x, y + 1, z);
            if (!blockState.getBlockType().hasBlockTag(BlockTags.WATER)) {
                return y + 1;
            }
            y++;
        }
        return y;
    }
}
