package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.component.EntityProjectileComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.world.sound.CustomSound;
import org.joml.Vector3d;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProjectileAttackExecutor extends RangedAttackExecutor {

    protected final Supplier<EntityType<?>> projectileType;
    protected final double projectileSpeed;
    protected final String sound;

    protected double arc = 0.2;
    protected Consumer<Entity> configurer = projectile -> {
    };

    public ProjectileAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                    double preferredRange, double minRange, boolean clearTargetAfterLose,
                                    int coolDown, Supplier<EntityType<?>> projectileType,
                                    double projectileSpeed, String sound) {
        super(targetIdMemory, speed, maxSenseRange, preferredRange, minRange, clearTargetAfterLose, coolDown);
        this.projectileType = projectileType;
        this.projectileSpeed = projectileSpeed;
        this.sound = sound;
    }

    public ProjectileAttackExecutor arc(double arc) {
        this.arc = arc;
        return this;
    }

    public ProjectileAttackExecutor configure(Consumer<Entity> configurer) {
        this.configurer = configurer;
        return this;
    }

    @Override
    protected int aimTicks() {
        return 1;
    }

    @Override
    protected void shoot(EntityIntelligent entity, Entity target) {
        var type = projectileType.get();
        if (type == null) {
            return;
        }

        var dimension = entity.getDimension();
        var location = entity.getLocation();
        var shootPos = new Vector3d(location.x(), location.y() + entity.getEyeHeight() - 0.1, location.z());
        var targetLoc = target.getLocation();
        var direction = new Vector3d(
                targetLoc.x() - shootPos.x(),
                targetLoc.y() + target.getEyeHeight() * aimHeightFactor() - shootPos.y(),
                targetLoc.z() - shootPos.z()
        );
        var distance = direction.length();
        if (distance < 1e-3) {
            return;
        }

        adjustDirection(direction, distance);
        direction.normalize();
        var projectile = type.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(shootPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(direction.mul(projectileSpeed))
                        .build()
        );
        if (projectile instanceof EntityProjectileComponent projectileComponent) {
            projectileComponent.setShooter(entity);
        }
        configurer.accept(projectile);
        dimension.getEntityManager().addEntity(projectile);
        if (sound != null) {
            dimension.addSound(shootPos, new CustomSound(sound));
        }
    }

    @Override
    protected double aimHeightFactor() {
        return 0.6;
    }

    @Override
    protected void adjustDirection(Vector3d direction, double distance) {
        direction.add(0.0, Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z()) * arc, 0.0);
    }
}
