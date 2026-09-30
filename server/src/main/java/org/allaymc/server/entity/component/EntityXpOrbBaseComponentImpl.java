package org.allaymc.server.entity.component;

import lombok.Getter;
import org.allaymc.api.container.Container;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.component.EntityXpOrbBaseComponent;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.enchantment.EnchantmentTypes;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.server.component.annotation.Dependency;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author daoge_cmd
 */
public class EntityXpOrbBaseComponentImpl extends EntityPickableBaseComponentImpl implements EntityXpOrbBaseComponent {

    protected static final String TAG_EXPERIENCE_VALUE = "ExperienceValue";
    protected static float MAX_MOVE_DISTANCE = 7.25f;
    protected static float MAX_MOVE_DISTANCE_SQUARED = MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE;

    @Dependency
    protected EntityPhysicsComponent physicsComponent;

    @Getter
    protected int experienceValue;

    public EntityXpOrbBaseComponentImpl(EntityInitInfo info) {
        super(info);
    }

    /** SkyBuild {@code ExperienceOrb::MERGE_*} (d315692). */
    protected static final double MERGE_BOX_SIZE = 1.0d;
    protected static final int MERGE_CHECK_PERIOD = 5;
    protected static final int MERGE_NEIGHBOR_CAP = 16;

    @Override
    public void tick(long currentTick) {
        super.tick(currentTick);
        moveToNearestPlayer();
        if (currentTick % MERGE_CHECK_PERIOD == 0) {
            tryMergeNearby();
        }
    }

    /**
     * SkyBuild {@code ExperienceOrb::tryMergeNearby}: 1x1x1 kutudaki deneyim kureleri tek kureye
     * birlesir (en dusuk runtime kimligi kalir), boylece cok sayida kucuk kure ortaligi doldurmaz.
     */
    protected void tryMergeNearby() {
        if (!isMergeable()) {
            return;
        }
        var half = MERGE_BOX_SIZE / 2.0d;
        var box = new org.joml.primitives.AABBd(
                location.x - half, location.y - half, location.z - half,
                location.x + half, location.y + half, location.z + half);
        var nearby = getDimension().getEntityManager().getPhysicsService()
                .computeCollidingEntities(box, entity -> entity != thisEntity && entity instanceof org.allaymc.api.entity.interfaces.EntityXpOrb);
        var seen = 0;
        for (var entity : nearby) {
            if (++seen > MERGE_NEIGHBOR_CAP) {
                return;
            }
            var other = (org.allaymc.api.entity.interfaces.EntityXpOrb) entity;
            if (other.getExperienceValue() <= 0 || other.isDead()) {
                continue;
            }
            // Kimligi kucuk olan kalir: yalnizca daha buyuk kimlikli kureyi bu kure yutar, digeri
            // kendi tikinde ayni karara varir.
            if (thisEntity.getRuntimeId() < other.getRuntimeId()) {
                absorb(other);
            }
        }
    }

    protected boolean isMergeable() {
        return experienceValue > 0 && !thisEntity.isDead() && thisEntity.isAlive();
    }

    /** {@code donor} kuresinin degerini bu kureye katar ve donor'u kaldirir. */
    protected void absorb(org.allaymc.api.entity.interfaces.EntityXpOrb donor) {
        var sum = (long) Math.max(0, this.experienceValue) + Math.max(0, donor.getExperienceValue());
        setExperienceValue((int) Math.max(1L, Math.min(sum, Integer.MAX_VALUE)));
        donor.setExperienceValue(0);
        donor.remove();
    }

    @Override
    public void onCollideWithEntity(Entity other) {
        if (this.experienceValue == 0 || !canBePicked() || !(other instanceof EntityPlayer player)
                || !player.isActualPlayer()) {
            return;
        }

        var remainingExperience = applyMending(player, this.experienceValue);
        player.addExperience(remainingExperience);
        this.setExperienceValue(0);
        remove();
    }

    protected int applyMending(EntityPlayer player, int experienceValue) {
        if (experienceValue <= 0) {
            return experienceValue;
        }

        var targets = collectMendingTargets(player);
        if (targets.isEmpty()) {
            return experienceValue;
        }

        var target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        var item = target.container().getItemStack(target.slot());

        var damage = item.getDamage();
        var repairAmount = Math.min(damage, experienceValue * 2);

        item.setDamage(damage - repairAmount);
        target.container().notifySlotChange(target.slot());

        var consumedExperience = (repairAmount + 1) / 2;
        return experienceValue - consumedExperience;
    }

    protected List<MendingTarget> collectMendingTargets(EntityPlayer player) {
        var targets = new ArrayList<MendingTarget>();
        var inventory = player.getContainer(ContainerTypes.INVENTORY);
        tryAddMendingTarget(targets, inventory, inventory.getHandSlot());
        tryAddMendingTarget(targets, player.getContainer(ContainerTypes.OFFHAND), 0);
        var armor = player.getContainer(ContainerTypes.ARMOR);
        for (int slot = 0; slot < armor.getItemStackArray().length; slot++) {
            tryAddMendingTarget(targets, armor, slot);
        }
        return targets;
    }

    protected void tryAddMendingTarget(List<MendingTarget> targets, Container container, int slot) {
        if (container != null && isMendingTarget(container.getItemStack(slot))) {
            targets.add(new MendingTarget(container, slot));
        }
    }

    protected boolean isMendingTarget(ItemStack item) {
        return item != ItemAirStack.AIR_STACK &&
               item.getItemType().getItemData().isDamageable() &&
               item.getDamage() > 0 &&
               item.hasEnchantment(EnchantmentTypes.MENDING);
    }

    protected record MendingTarget(Container container, int slot) {
    }

    protected void moveToNearestPlayer() {
        var nearestPlayer = findNearestPlayer();
        if (nearestPlayer == null) return;

        var playerLoc = nearestPlayer.getLocation();
        var dX = (playerLoc.x() - this.location.x) / 8f;
        var dY = (playerLoc.y() + nearestPlayer.getEyeHeight() / 2f - this.location.y) / 8f;
        var dZ = (playerLoc.z() - this.location.z) / 8f;
        var d = (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        var diff = 1f - d;

        if (diff > 0D) {
            diff = diff * diff;
            physicsComponent.addMotion(
                    dX / d * diff * 0.1f,
                    dY / d * diff * 0.1f,
                    dZ / d * diff * 0.1f
            );
        }
    }

    protected EntityPlayer findNearestPlayer() {
        EntityPlayer nearestPlayer = null;
        var nearestDistanceSquared = MAX_MOVE_DISTANCE_SQUARED;
        for (var player : getDimension().getPlayers()) {
            var entity = player.getControlledEntity();
            var distanceSquared = entity.getLocation().distanceSquared(location);
            if (distanceSquared < nearestDistanceSquared) {
                nearestPlayer = entity;
            }
        }
        return nearestPlayer;
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForInt(TAG_EXPERIENCE_VALUE, this::setExperienceValue);
    }

    @Override
    public NbtMap saveNBT() {
        return super.saveNBT()
                .toBuilder()
                .putInt(TAG_EXPERIENCE_VALUE, this.experienceValue)
                .build();
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.05, 0, -0.05, 0.05, 0.1, 0.05);
    }

    @Override
    public void setExperienceValue(int experienceValue) {
        this.experienceValue = experienceValue;
        broadcastState();
    }
}
