package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.server.entity.component.sulfurcube.EntitySulfurCubeBaseComponentImpl;
import org.allaymc.server.entity.component.sulfurcube.SulfurCubeArchetypes;
import org.joml.Vector3d;

/**
 * Sulfur kupunun emilebilir bir bloga yaklasip onu yutma davranisi.
 *
 * <p>Hedef iki turlu olabilir: yere dusmus bir esya ya da o blogu elinde tutan bir oyuncu. Ikisine
 * de ayni sekilde yaklasilir, ama yalnizca yerdeki esya gercekten yutulur — oyuncunun elindekini
 * kup kendiliginden alamaz, onu ancak oyuncu uzatirsa alir. Oyuncu burada yalnizca kupu pesinden
 * suruklemeye yarar, wiki de "elinde uygun blok tutan oyuncuyu takip eder" diyor.</p>
 */
public class AbsorbBlockExecutor implements BehaviorExecutor {

    /** Yerdeki esyanin yutulabilecegi mesafe (blok). */
    protected static final double ABSORB_RANGE_SQUARED = 1.5 * 1.5;

    protected final float speed;
    protected final double maxRangeSquared;

    protected Vector3d lastTargetPos;

    /**
     * @param speed hedefe giderken kullanilan hiz
     * @param maxRange hedefin takip edilebilecegi en fazla mesafe (blok)
     */
    public AbsorbBlockExecutor(float speed, double maxRange) {
        this.speed = speed;
        this.maxRangeSquared = maxRange * maxRange;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        lastTargetPos = null;
        entity.setMovementSpeed(speed);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        if (!(entity instanceof EntitySulfurCubeBaseComponent cube)
            || !cube.isLarge() || cube.getAbsorbedBlock() != null || cube.isPickupOnCooldown()) {
            return false;
        }

        var targetId = entity.getMemoryStorage().get(MemoryTypes.NEAREST_ABSORBABLE);
        if (targetId == null) {
            return false;
        }

        var target = entity.getDimension().getEntityManager().getEntity(targetId);
        if (target == null || !target.isAlive()) {
            return false;
        }

        var targetLoc = target.getLocation();
        var distanceSquared = entity.getLocation().distanceSquared(targetLoc);
        if (distanceSquared > maxRangeSquared) {
            return false;
        }

        if (entity.getMovementSpeed() != speed) {
            entity.setMovementSpeed(speed);
        }

        EntityControlHelper.setLookTarget(entity, new Vector3d(
                targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()));

        var moveTarget = new Vector3d(targetLoc.x(), targetLoc.y(), targetLoc.z());
        entity.setMoveTarget(moveTarget);
        if (lastTargetPos == null || isInDifferentBlock(lastTargetPos, moveTarget)) {
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }
        lastTargetPos = moveTarget;

        if (distanceSquared <= ABSORB_RANGE_SQUARED) {
            return !tryAbsorb(entity, cube, target);
        }

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        lastTargetPos = null;
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    /**
     * Yerdeki bir esyayi yutmayi dener.
     *
     * @return blok gercekten emildiyse {@code true}
     */
    protected boolean tryAbsorb(EntityIntelligent entity, EntitySulfurCubeBaseComponent cube, Entity target) {
        // Oyuncunun elindekine dokunulmaz; kup yalnizca onu takip eder.
        if (target instanceof EntityPlayer) {
            return false;
        }

        if (!(target instanceof EntityItem item)) {
            return false;
        }

        var blockType = blockTypeOf(item.getItemStack());
        if (blockType == null) {
            return false;
        }

        cube.setAbsorbedBlock(blockType.getDefaultState());

        // Yigindan yalnizca bir blok alinir. Once esyanin tamami siliniyordu; yere dusmus altmis
        // dort cimenin altmis ucu, kup bir tanesini yuttugu anda yok oluyordu.
        var stack = item.getItemStack();
        if (stack.getCount() > 1) {
            stack.reduceCount(1);
            item.setItemStack(stack);
        } else {
            target.remove();
        }

        if (cube instanceof EntitySulfurCubeBaseComponentImpl impl) {
            impl.playAbsorbSound();
        }
        return true;
    }

    protected org.allaymc.api.block.type.BlockType<?> blockTypeOf(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }

        var blockType = itemStack.getItemType().getBlockType();
        return blockType != null && SulfurCubeArchetypes.isAbsorbable(blockType) ? blockType : null;
    }

    protected boolean isInDifferentBlock(Vector3d oldTargetPos, Vector3d newTargetPos) {
        return Math.floor(oldTargetPos.x()) != Math.floor(newTargetPos.x()) ||
               Math.floor(oldTargetPos.y()) != Math.floor(newTargetPos.y()) ||
               Math.floor(oldTargetPos.z()) != Math.floor(newTargetPos.z());
    }
}
