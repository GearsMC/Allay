package org.allaymc.server.entity.component.projectile;

import org.allaymc.api.block.dto.Block;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityThrownTridentBaseComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.event.world.LightningStrikeEvent;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.data.Weather;
import org.allaymc.api.world.sound.SimpleSound;
import org.allaymc.server.component.annotation.Dependency;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Physics component implementation for thrown trident entities.
 *
 * @author daoge_cmd
 */
public class EntityThrownTridentPhysicsComponentImpl extends EntityProjectilePhysicsComponentImpl {

    /**
     * The base damage dealt by the trident.
     */
    protected static final float BASE_DAMAGE = 11.0f;

    /** PHP {@code MAX_BONUS_DAMAGE}: mesafe bonusunun ust siniri. */
    protected static final float MAX_BONUS_DAMAGE = 12.0f;

    /** PHP {@code MAX_BONUS_DISTANCE}: bonusun doldugu mesafe (blok). */
    protected static final float MAX_BONUS_DISTANCE = 35.0f;

    /** PHP {@code BONUS_DAMAGE_ON_RETURN_HIT}: 25 blok sonrasi donus isabeti bonusu. */
    protected static final float RETURN_HIT_DISTANCE = 25.0f;

    /** PHP {@code BONUS_DAMAGE_ON_RETURN_HIT}. */
    protected static final float RETURN_HIT_BONUS = 2.0f;

    /** PHP {@code LOYALTY_AIR_RETURN_TICKS}: havada bu kadar tik sonra doner. */
    protected static final int AIR_RETURN_TICKS = 20;

    @Dependency
    protected EntityThrownTridentBaseComponent tridentBaseComponent;
    @Dependency
    protected org.allaymc.api.entity.component.EntityAgeComponent ageComponent;

    // Indicates whether the trident has already hit a block (like arrow's hitBlock)
    protected boolean hitBlock;

    /** Firlatildigi konum; mesafe bonusu icin (PHP {@code startPosition}). */
    protected Vector3d startPosition;

    @Override
    public double getGravity() {
        return 0.05;
    }

    @Override
    public double getWaterDragFactor() {
        return 0.01;
    }

    @Override
    public double getLavaDragFactor() {
        return 0.01;
    }

    @Override
    public Vector3d updateMotion(LiquidState liquidState) {
        // If returning to shooter, use special motion logic
        if (tridentBaseComponent.isReturning()) {
            return updateReturningMotion();
        }

        if (startPosition == null) {
            startPosition = new Vector3d(thisEntity.getLocation());
        }

        if (shouldReturnFromVoid()) {
            startReturning();
            return updateReturningMotion();
        }

        // PHP LOYALTY_AIR_RETURN_TICKS: sadakatli trident havada 20 tik sonra
        // sahibine doner (yalnizca sadakat buyusu varsa; startReturning zaten
        // buyu yoksa donusu baslatmaz).
        if (tridentBaseComponent.getLoyaltyLevel() > 0
                && !tridentBaseComponent.isReturning()
                && ageComponent.getAge() >= AIR_RETURN_TICKS) {
            startReturning();
            return updateReturningMotion();
        }

        if (liquidState.inLiquid() && computeLiquidPhysics()) {
            return updateMotionInLiquid(liquidState);
        }

        // Same as arrow: if hit block and still colliding with blocks, no motion
        if (hitBlock && tridentBaseComponent.checkBlockCollision()) {
            return new Vector3d(0, 0, 0);
        }

        // Normal physics (including when block is removed - trident will fall)
        return new Vector3d(
                this.motion.x * (1 - this.getDragFactorInAir()),
                (this.motion.y - this.getGravity()) * (1 - this.getDragFactorInAir()),
                this.motion.z * (1 - this.getDragFactorInAir())
        );
    }

    /**
     * Updates motion when the trident is returning to the shooter.
     * Uses percentage-based movement:
     * - Moves a percentage of remaining distance each tick
     * - Creates natural deceleration as trident approaches player
     */
    protected Vector3d updateReturningMotion() {
        var shooter = projectileComponent.getShooter();
        if (!canReturnToShooter(shooter)) {
            // Shooter is gone, drop the trident as item entity
            dropTridentItem();
            thisEntity.remove();
            return new Vector3d(0, 0, 0);
        }

        // If shooter is in different dimension, wait (don't move)
        if (shooter.getDimension() != thisEntity.getDimension()) {
            return new Vector3d(0, 0, 0);
        }

        var location = thisEntity.getLocation();
        var shooterPos = shooter.getLocation();

        // Calculate direction to shooter's eye level
        var direction = new Vector3d(
                shooterPos.x() - location.x(),
                shooterPos.y() - location.y() + shooter.getEyeHeight(),
                shooterPos.z() - location.z()
        );

        var distance = direction.length();

        // Check if close enough to be picked up (distance < 1.5)
        if (distance < 1.5 && shooter instanceof EntityPlayer player) {
            giveTridentToPlayer(player);
            thisEntity.remove();
            return new Vector3d(0, 0, 0);
        }

        // Percentage-based movement, higher loyalty = faster return
        var loyaltyLevel = tridentBaseComponent.getLoyaltyLevel();
        var force = 0.05 * loyaltyLevel;

        // Calculate velocity (percentage of remaining distance)
        // Motion will be applied by applyMotionNoClip(), so don't update position here
        return new Vector3d(
                direction.x * force,
                direction.y * force,
                direction.z * force
        );
    }

    /**
     * Gives the trident item back to the player.
     * Prefers to return to the favored slot if available and empty.
     */
    protected void giveTridentToPlayer(EntityPlayer player) {
        var tridentItem = tridentBaseComponent.getTridentItem();
        if (tridentItem == null || tridentItem.getItemType() == ItemTypes.AIR) {
            return;
        }

        var favoredSlot = tridentBaseComponent.getFavoredSlot();
        if (favoredSlot >= 0) {
            var inventory = player.getContainer(ContainerTypes.INVENTORY);
            var slotItem = inventory.getItemStack(favoredSlot);
            // If the favored slot is empty, put the trident there
            if (slotItem.getItemType() == ItemTypes.AIR) {
                inventory.setItemStack(favoredSlot, tridentItem);
                return;
            }
            // If the favored slot already has a trident (creative mode), don't add another one
            if (slotItem.getItemType() == ItemTypes.TRIDENT) {
                return;
            }
        }

        // Otherwise, try to add to inventory normally
        player.tryAddItem(tridentItem);
    }

    /**
     * Drops the trident as an item entity.
     */
    protected void dropTridentItem() {
        var tridentItem = tridentBaseComponent.getTridentItem();
        if (tridentItem != null && tridentItem.getItemType() != ItemTypes.AIR) {
            thisEntity.getDimension().dropItem(tridentItem, thisEntity.getLocation());
        }
    }

    @Override
    protected void onHitEntity(Entity other, Vector3dc hitPos) {
        if (thisEntity.willBeDespawnedLater()) {
            return;
        }

        // If returning, ignore collisions
        if (tridentBaseComponent.isReturning()) {
            return;
        }

        addHitSound(hitPos, false);

        if (other instanceof EntityLiving living) {
            float damage = BASE_DAMAGE;
            // PHP TridentEntity: mesafe bonusu (35 blokta dolar, en cok +12) ve
            // 25 blok sonrasi +2 donus isabeti bonusu.
            float traveled = traveledDistance();
            damage += Math.min(traveled / MAX_BONUS_DISTANCE, 1.0f) * MAX_BONUS_DAMAGE;
            if (traveled >= RETURN_HIT_DISTANCE) {
                damage += RETURN_HIT_BONUS;
            }

            // Apply impaling damage bonus in water or rain
            var impalingLevel = tridentBaseComponent.getImpalingLevel();
            if (impalingLevel > 0) {
                if (other.isTouchingWater() || isEntityInRain(other)) {
                    damage += 2.5f * impalingLevel;
                }
            }

            var damageContainer = DamageContainer.projectile(thisEntity, damage);
            // Use the last location as the knockback source
            damageContainer.setKnockbackSource(hitPos.sub(this.motion, new Vector3d()));

            if (living.attack(damageContainer)) {
                // Apply additional durability cost for hitting entity
                applyHitDurabilityCost();
            }
        }

        // Handle channeling enchantment
        handleChanneling(hitPos);

        // Handle collision result
        handlePostCollision();
    }

    @Override
    protected void onHitBlock(Block block, Vector3dc hitPos) {
        if (thisEntity.willBeDespawnedLater() || this.hitBlock) {
            return;
        }

        // If returning, ignore block collisions
        if (tridentBaseComponent.isReturning()) {
            return;
        }

        addHitSound(hitPos, true);
        this.hitBlock = true;

        // Check if should return to shooter
        if (canReturnToShooter(projectileComponent.getShooter())) {
            startReturning();
        }
    }

    /**
     * Handles the channeling enchantment effect.
     * Only summons lightning when hitting an entity during a thunderstorm.
     */
    protected void handleChanneling(Vector3dc hitPos) {
        if (!tridentBaseComponent.hasChanneling()) {
            return;
        }

        var dimension = thisEntity.getDimension();
        var world = dimension.getWorld();

        // Only works during thunderstorm and if position can see sky
        if (world.getWeather() != Weather.THUNDER) {
            return;
        }

        var pos = thisEntity.getLocation();
        if (!dimension.canPosSeeSky((int) pos.x(), (int) pos.y(), (int) pos.z())) {
            return;
        }

        // Spawn lightning bolt using the dimension's strikeLightning method
        if (dimension.strikeLightning(hitPos.x(), hitPos.y(), hitPos.z(), LightningStrikeEvent.Cause.TRIDENT)) {
            dimension.addSound(hitPos, SimpleSound.TRIDENT_THUNDER, false);
        }
    }

    /**
     * Handles post-collision behavior.
     */
    protected void handlePostCollision() {
        this.hitBlock = true;

        // Check if should return to shooter
        if (canReturnToShooter(projectileComponent.getShooter())) {
            startReturning();
        }
    }

    /**
     * Sadakatli trident boşluğa düşünce geri döner (GearsMC fork; HeartCore e1a2ab23).
     *
     * <p>Yukarı akışta dönüş yalnızca bir bloğa ya da varlığa çarpınca başlıyordu; adanın kenarından
     * atılan trident hiçbir şeye çarpmadan dünyanın altına düşüp kayboluyordu. Hiçbir yere saplanmamış
     * trident dünya tabanının bir blok üstüne indiğinde ya da bir sonraki konumu yüklü olmayan bir
     * chunk'a düştüğünde dönüşe geçer.</p>
     */
    protected boolean shouldReturnFromVoid() {
        if (this.hitBlock || !canReturnToShooter(projectileComponent.getShooter())) {
            return false;
        }
        var dimension = thisEntity.getDimension();
        var location = thisEntity.getLocation();
        if (location.y() <= dimension.getDimensionType().getMinHeight() + 1) {
            return true;
        }
        int nextX = (int) Math.floor(location.x() + this.motion.x);
        int nextZ = (int) Math.floor(location.z() + this.motion.z);
        return !dimension.getChunkManager().isChunkLoaded(nextX >> 4, nextZ >> 4);
    }

    /** PHP {@code traveledDistance}: firlatildigi noktadan bu yana alinan yol. */
    protected float traveledDistance() {
        if (startPosition == null) {
            return 0.0f;
        }
        return (float) startPosition.distance(thisEntity.getLocation());
    }

    /**
     * Starts the returning phase.
     */
    protected void startReturning() {
        thisEntity.getDimension().addSound(thisEntity.getLocation(), SimpleSound.TRIDENT_RETURN, false);
        tridentBaseComponent.setReturning(true);
    }

    /**
     * Checks if the trident can return to the shooter.
     */
    protected boolean canReturnToShooter(Entity shooter) {
        if (tridentBaseComponent.getLoyaltyLevel() <= 0) {
            return false;
        }

        if (shooter == null || !shooter.isAlive()) {
            return false;
        }

        if (shooter instanceof EntityPlayer player) {
            return player.getGameMode() != GameMode.SPECTATOR;
        }

        return true;
    }

    /**
     * Checks if an entity is being rained on.
     */
    protected boolean isEntityInRain(Entity entity) {
        var dimension = entity.getDimension();
        var world = dimension.getWorld();

        if (world.getWeather() == Weather.CLEAR) {
            return false;
        }

        var pos = entity.getLocation();
        return dimension.canPosSeeSky((int) pos.x(), (int) pos.y(), (int) pos.z());
    }

    protected void addHitSound(Vector3dc hitPos, boolean hitGround) {
        var sound = hitGround ? SimpleSound.TRIDENT_HIT_GROUND : SimpleSound.TRIDENT_HIT;
        thisEntity.getDimension().addSound(hitPos, sound, false);
    }

    /**
     * Applies additional durability cost when hitting an entity.
     * The trident costs +1 durability when it hits an entity.
     */
    protected void applyHitDurabilityCost() {
        var tridentItem = tridentBaseComponent.getTridentItem();
        if (tridentItem != null && tridentItem.getItemType() != ItemTypes.AIR) {
            tridentItem.tryIncreaseDamage(1);
        }
    }

    @Override
    public boolean applyMotion() {
        // When returning, the trident should pass through blocks (noClip)
        if (tridentBaseComponent.isReturning()) {
            return applyMotionNoClip();
        }
        return super.applyMotion();
    }

    /**
     * Apply motion without block collision detection (for returning tridents).
     * The trident will only check for entity collisions when returning.
     */
    protected boolean applyMotionNoClip() {
        if (motion.lengthSquared() == 0) {
            return false;
        }

        var location = thisEntity.getLocation();
        var newPos = new Location3d(location);
        newPos.add(motion);

        if (newPos.distance(location) > 0) {
            computeRotationFromMotion(newPos, this.motion);
        }

        return !newPos.equals(location) && thisEntity.trySetLocation(newPos);
    }

    @Override
    protected boolean shouldSkipEntityCollision(Entity entity) {
        if (super.shouldSkipEntityCollision(entity)) {
            return true;
        }

        // Skip collision when returning
        return tridentBaseComponent.isReturning();
    }
}
