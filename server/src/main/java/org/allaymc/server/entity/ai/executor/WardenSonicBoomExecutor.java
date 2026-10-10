package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.mob.EntityWardenBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.joml.Vector3d;

public class WardenSonicBoomExecutor implements BehaviorExecutor {

    public static final double MIN_RANGE = 4;
    public static final double MAX_RANGE = 15;
    public static final int COOLDOWN = 40;

    protected static final int CHARGE_TICKS = 34;
    protected static final float DAMAGE = 10;
    protected static final double KNOCKBACK = 2.5;
    protected static final double KNOCKBACK_VERTICAL = 0.5;

    protected final MemoryType<Long> targetIdMemory;

    protected int tick;
    protected boolean fired;

    public WardenSonicBoomExecutor(MemoryType<Long> targetIdMemory) {
        this.targetIdMemory = targetIdMemory;
    }

    public static boolean canBoom(EntityIntelligent entity, MemoryType<Long> targetIdMemory) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        if (targetId == null || !(((EntityImpl) entity).getBaseComponent() instanceof EntityWardenBaseComponentImpl warden)
                || entity.getTick() < warden.getNextSonicBoomTick()) {
            return false;
        }

        var target = entity.getDimension().getEntityManager().getEntity(targetId);
        if (target == null || !target.isAlive()) {
            return false;
        }

        var distanceSquared = entity.getLocation().distanceSquared(target.getLocation());
        return distanceSquared > MIN_RANGE * MIN_RANGE && distanceSquared <= MAX_RANGE * MAX_RANGE;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tick = 0;
        fired = false;
        EntityControlHelper.removeRouteTarget(entity);
        warden(entity).setSonicCharging(true);
        entity.getDimension().addSound(entity.getLocation(), new CustomSound(SoundNames.MOB_WARDEN_SONIC_CHARGE));
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityLiving living) || !living.isAlive()
                || !EntityControlHelper.allowsTarget(entity, target)) {
            return false;
        }

        var targetLoc = target.getLocation();
        EntityControlHelper.setLookTarget(entity, new Vector3d(targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()));
        if (++tick < CHARGE_TICKS) {
            return true;
        }

        boom(entity, living);
        fired = true;
        return false;
    }

    protected void boom(EntityIntelligent entity, EntityLiving target) {
        var dimension = entity.getDimension();
        var loc = entity.getLocation();
        var from = new Vector3d(loc.x(), loc.y() + 1.6, loc.z());
        var targetLoc = target.getLocation();
        var to = new Vector3d(targetLoc.x(), targetLoc.y() + target.getEyeHeight() * 0.5, targetLoc.z());
        var direction = new Vector3d(to).sub(from);
        var length = direction.length();
        if (length > 1e-3) {
            direction.div(length);
            for (var step = 1; step < length; step++) {
                dimension.addParticle(new Vector3d(direction).mul(step).add(from), SimpleParticle.SONIC_EXPLOSION);
            }
        }
        dimension.addSound(from, new CustomSound(SoundNames.MOB_WARDEN_SONIC_BOOM));

        var damage = DamageContainer.entityAttack(entity, DAMAGE);
        damage.setIgnoreReduction(true);
        damage.setKnockback(KNOCKBACK);
        damage.setKnockbackVertical(KNOCKBACK_VERTICAL);
        target.attack(damage, true);
    }

    protected EntityWardenBaseComponentImpl warden(EntityIntelligent entity) {
        return (EntityWardenBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        var warden = warden(entity);
        warden.setSonicCharging(false);
        warden.setNextSonicBoomTick(entity.getTick() + (fired ? COOLDOWN : 20));
        EntityControlHelper.removeLookTarget(entity);
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }
}
