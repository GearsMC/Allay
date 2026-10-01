package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.joml.Vector3d;

/**
 * Hedeften uzaga kacma — HeartCore {@code Rabbit::performFlee}.
 *
 * <p>Her tick hedefin tersi yonde {@code fleeDistance} blok ilerideki noktaya yurur. Hedef
 * gorus alanindan ciksa bile son bilinen konumdan {@code lingerTicks} tick kacmaya devam eder
 * (PHP {@code FLEE_DURATION = 60}).</p>
 */
public class FleeFromTargetExecutor implements BehaviorExecutor {

    protected final MemoryType<Long> targetIdMemory;
    protected final float speed;
    protected final double fleeDistance;
    protected final int lingerTicks;

    protected int linger;
    protected Vector3d threat;
    protected Vector3d lastMoveTarget;

    /**
     * @param targetIdMemory kacilacak varligin calisma zamani kimligi
     * @param speed kacis hizi
     * @param fleeDistance her adimda hedeflenen kacis mesafesi (blok)
     * @param lingerTicks hedef kaybolunca kacisin surdugu tick sayisi
     */
    public FleeFromTargetExecutor(MemoryType<Long> targetIdMemory, float speed, double fleeDistance, int lingerTicks) {
        this.targetIdMemory = targetIdMemory;
        this.speed = speed;
        this.fleeDistance = fleeDistance;
        this.lingerTicks = lingerTicks;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        linger = lingerTicks;
        threat = null;
        lastMoveTarget = null;
        entity.setMovementSpeed(speed);
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        if (target != null && target.isAlive() && EntityControlHelper.allowsTarget(entity, target)) {
            var loc = target.getLocation();
            threat = new Vector3d(loc.x(), loc.y(), loc.z());
            linger = lingerTicks;
        } else if (--linger <= 0 || threat == null) {
            return false;
        }

        if (entity.getMovementSpeed() != speed) {
            entity.setMovementSpeed(speed);
        }
        var self = entity.getLocation();
        double dx = self.x() - threat.x;
        double dz = self.z() - threat.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1e-4) {
            dx = 1;
            dz = 0;
            length = 1;
        }
        var moveTarget = new Vector3d(self.x() + dx / length * fleeDistance, self.y(), self.z() + dz / length * fleeDistance);
        entity.setMoveTarget(moveTarget);
        if (lastMoveTarget == null || differentBlock(lastMoveTarget, moveTarget)) {
            entity.getBehaviorGroup().setRouteUpdateRequired(true);
        }
        lastMoveTarget = moveTarget;
        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        entity.setMovementSpeed(MemoryTypes.MOVEMENT_SPEED.defaultData().get());
        threat = null;
        lastMoveTarget = null;
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }

    protected static boolean differentBlock(Vector3d a, Vector3d b) {
        return Math.floor(a.x) != Math.floor(b.x) || Math.floor(a.y) != Math.floor(b.y) || Math.floor(a.z) != Math.floor(b.z);
    }
}
