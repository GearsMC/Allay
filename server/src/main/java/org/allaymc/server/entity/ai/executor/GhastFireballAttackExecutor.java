package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.joml.Vector3d;

public class GhastFireballAttackExecutor extends FireballAttackExecutor {

    protected static final double GHAST_FIREBALL_SPEED = 1.6;

    public GhastFireballAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                       double preferredRange, double minRange, boolean clearTargetAfterLose,
                                       int chargeTime, int coolDown, double fireRange) {
        super(targetIdMemory, speed, maxSenseRange, preferredRange, minRange, clearTargetAfterLose,
                chargeTime, coolDown, 1, fireRange);
    }

    @Override
    protected void shoot(EntityIntelligent entity, Entity target) {
        var dimension = entity.getDimension();
        var location = entity.getLocation();
        var look = new Vector3d(target.getLocation().x() - location.x(), 0, target.getLocation().z() - location.z());
        if (look.lengthSquared() > 1e-6) {
            look.normalize(2.0);
        }
        var shootPos = new Vector3d(location.x() + look.x(), location.y() + entity.getEyeHeight() * 0.5, location.z() + look.z());
        var targetLoc = target.getLocation();
        var direction = new Vector3d(
                targetLoc.x() - shootPos.x(),
                targetLoc.y() + target.getEyeHeight() * 0.5 - shootPos.y(),
                targetLoc.z() - shootPos.z()
        );
        if (direction.lengthSquared() < 1e-6) {
            return;
        }

        direction.normalize();
        var fireball = EntityTypes.FIREBALL.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(shootPos)
                        .rot(-location.yaw(), -location.pitch())
                        .motion(direction.mul(GHAST_FIREBALL_SPEED))
                        .build()
        );
        fireball.setShooter(entity);
        dimension.getEntityManager().addEntity(fireball);
        dimension.addSound(shootPos, new CustomSound(SoundNames.MOB_GHAST_FIREBALL));
    }
}
