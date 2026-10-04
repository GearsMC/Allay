package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.entity.interfaces.EntityVex;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.entity.component.mob.EntityEvocationFangBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityEvokerBaseComponentImpl;
import org.allaymc.server.entity.component.mob.EntityVexBaseComponentImpl;
import org.allaymc.server.entity.impl.EntityImpl;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;

import java.util.concurrent.ThreadLocalRandom;

public class EvokerSpellExecutor implements BehaviorExecutor {

    public enum Spell {
        FANGS,
        SUMMON_VEX
    }

    public static final int FANG_COLOR = 0xFF664D59;
    public static final int SUMMON_COLOR = 0xFFB3B3CC;
    public static final int FANG_COOLDOWN = 100;
    public static final int SUMMON_COOLDOWN = 340;
    public static final int VEX_CAP = 8;
    public static final double VEX_CAP_RADIUS = 16;

    protected static final int FANG_CAST_TICKS = 40;
    protected static final int FANG_RELEASE_TICK = 20;
    protected static final int SUMMON_CAST_TICKS = 100;
    protected static final double CIRCLE_RANGE = 3;
    protected static final int LINE_COUNT = 16;
    protected static final double LINE_SPACING = 1.25;
    protected static final int VEX_COUNT = 3;

    protected final MemoryType<Long> targetIdMemory;
    protected final Spell spell;

    protected int tick;
    protected boolean completed;

    public EvokerSpellExecutor(MemoryType<Long> targetIdMemory, Spell spell) {
        this.targetIdMemory = targetIdMemory;
        this.spell = spell;
    }

    public static int countVexes(Entity evoker) {
        var loc = evoker.getLocation();
        var box = new AABBd(loc.x() - VEX_CAP_RADIUS, loc.y() - VEX_CAP_RADIUS, loc.z() - VEX_CAP_RADIUS,
                loc.x() + VEX_CAP_RADIUS, loc.y() + VEX_CAP_RADIUS, loc.z() + VEX_CAP_RADIUS);
        return evoker.getDimension().getEntityManager().getPhysicsService()
                .computeCollidingEntities(box, e -> e instanceof EntityVex && e.isAlive()).size();
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tick = 0;
        completed = false;
        EntityControlHelper.removeRouteTarget(entity);
        evoker(entity).startCasting(spell == Spell.FANGS ? FANG_COLOR : SUMMON_COLOR);
        entity.getDimension().addSound(entity.getLocation(), new CustomSound(spell == Spell.FANGS
                ? SoundNames.MOB_EVOCATION_ILLAGER_PREPARE_ATTACK
                : SoundNames.MOB_EVOCATION_ILLAGER_PREPARE_SUMMON));
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(targetIdMemory);
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        if (!(target instanceof EntityLiving) || !target.isAlive() || !EntityControlHelper.allowsTarget(entity, target)) {
            return false;
        }

        var targetLoc = target.getLocation();
        EntityControlHelper.setLookTarget(entity, new Vector3d(targetLoc.x(), targetLoc.y() + target.getEyeHeight(), targetLoc.z()));
        tick++;
        if (spell == Spell.FANGS) {
            if (tick == FANG_RELEASE_TICK) {
                castFangs(entity, target);
                entity.getDimension().addSound(entity.getLocation(), new CustomSound(SoundNames.MOB_EVOCATION_ILLAGER_CAST_SPELL));
            }
            if (tick >= FANG_CAST_TICKS) {
                completed = true;
                return false;
            }
            return true;
        }

        if (tick >= SUMMON_CAST_TICKS) {
            summonVexes(entity);
            entity.getDimension().addSound(entity.getLocation(), new CustomSound(SoundNames.MOB_EVOCATION_ILLAGER_CAST_SPELL));
            completed = true;
            return false;
        }
        return true;
    }

    protected void castFangs(EntityIntelligent entity, Entity target) {
        var loc = entity.getLocation();
        var targetLoc = target.getLocation();
        var minY = Math.min(loc.y(), targetLoc.y());
        var maxY = Math.max(loc.y(), targetLoc.y()) + 1;
        var dx = targetLoc.x() - loc.x();
        var dz = targetLoc.z() - loc.z();
        var distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < CIRCLE_RANGE) {
            for (var i = 0; i < 5; i++) {
                var angle = Math.toRadians(loc.yaw()) + i * Math.PI * 2 / 5;
                spawnFang(entity, loc.x() + Math.cos(angle) * 1.5, loc.z() + Math.sin(angle) * 1.5, minY, maxY, 0);
            }
            for (var i = 0; i < 8; i++) {
                var angle = Math.toRadians(loc.yaw()) + i * Math.PI * 2 / 8 + Math.PI * 2 / 5;
                spawnFang(entity, loc.x() + Math.cos(angle) * 2.5, loc.z() + Math.sin(angle) * 2.5, minY, maxY, 3);
            }
            return;
        }

        var nx = dx / distance;
        var nz = dz / distance;
        for (var i = 0; i < LINE_COUNT; i++) {
            var step = LINE_SPACING * (i + 1);
            spawnFang(entity, loc.x() + nx * step, loc.z() + nz * step, minY, maxY, i);
        }
    }

    protected void spawnFang(EntityIntelligent owner, double x, double z, double minY, double maxY, int warmup) {
        var dimension = owner.getDimension();
        var y = findFangY(dimension, x, z, minY, maxY);
        if (Double.isNaN(y)) {
            return;
        }

        var fang = EntityTypes.EVOCATION_FANG.createEntity(EntityInitInfo.builder()
                .dimension(dimension)
                .pos(x, y, z)
                .rot(owner.getLocation().yaw(), 0)
                .build());
        if (fang instanceof EntityImpl impl && impl.getBaseComponent() instanceof EntityEvocationFangBaseComponentImpl base) {
            base.setWarmupTicks(warmup);
            base.setOwnerRuntimeId(owner.getRuntimeId());
        }
        dimension.getEntityManager().addEntity(fang);
    }

    protected static double findFangY(Dimension dimension, double x, double z, double minY, double maxY) {
        var bx = (int) Math.floor(x);
        var bz = (int) Math.floor(z);
        for (var by = (int) Math.floor(maxY); by >= (int) Math.floor(minY) - 1; by--) {
            var below = dimension.getBlockState(bx, by - 1, bz).getBlockStateData();
            var here = dimension.getBlockState(bx, by, bz).getBlockStateData();
            if (below.hasCollision() && !here.hasCollision()) {
                return by;
            }
        }
        return Double.NaN;
    }

    protected void summonVexes(EntityIntelligent entity) {
        var dimension = entity.getDimension();
        var loc = entity.getLocation();
        var rand = ThreadLocalRandom.current();
        for (var i = 0; i < VEX_COUNT && countVexes(entity) + i < VEX_CAP; i++) {
            var vex = EntityTypes.VEX.createEntity(EntityInitInfo.builder()
                    .dimension(dimension)
                    .pos(loc.x() + rand.nextDouble(-1, 1), loc.y() + 1 + rand.nextDouble(0, 1), loc.z() + rand.nextDouble(-1, 1))
                    .rot(loc.yaw(), 0)
                    .build());
            if (vex instanceof EntityImpl impl && impl.getBaseComponent() instanceof EntityVexBaseComponentImpl base) {
                base.bindOwner(entity);
            }
            dimension.getEntityManager().addEntity(vex);
        }
    }

    protected EntityEvokerBaseComponentImpl evoker(EntityIntelligent entity) {
        return (EntityEvokerBaseComponentImpl) ((EntityImpl) entity).getBaseComponent();
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        var base = evoker(entity);
        base.stopCasting();
        EntityControlHelper.removeLookTarget(entity);
        var now = entity.getTick();
        if (spell == Spell.FANGS) {
            base.setNextFangCastTick(now + (completed ? FANG_COOLDOWN : 20));
        } else {
            base.setNextSummonTick(now + (completed ? SUMMON_COOLDOWN : 40));
        }
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }
}
