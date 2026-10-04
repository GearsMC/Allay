package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.behavior.BehaviorExecutor;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;

import java.util.concurrent.ThreadLocalRandom;

public class BreezeJumpExecutor implements BehaviorExecutor {

    protected static final double MIN_VERTICAL = 0.7;
    protected static final double MAX_VERTICAL = 1.0;
    protected static final double HORIZONTAL = 0.45;

    @Override
    public boolean execute(EntityIntelligent entity) {
        if (!entity.isOnGround()) {
            return false;
        }

        var rand = ThreadLocalRandom.current();
        var loc = entity.getLocation();
        var angle = rand.nextDouble(Math.PI * 2);
        var targetId = entity.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET);
        if (targetId == null) {
            targetId = entity.getMemoryStorage().get(MemoryTypes.NEAREST_PLAYER);
        }
        var target = targetId == null ? null : entity.getDimension().getEntityManager().getEntity(targetId);
        if (target != null) {
            var targetLoc = target.getLocation();
            angle = Math.atan2(targetLoc.z() - loc.z(), targetLoc.x() - loc.x()) + rand.nextDouble(-Math.PI / 2, Math.PI / 2);
        }

        entity.setMotion(Math.cos(angle) * HORIZONTAL, rand.nextDouble(MIN_VERTICAL, MAX_VERTICAL), Math.sin(angle) * HORIZONTAL);
        entity.getDimension().addSound(loc, new CustomSound(SoundNames.MOB_BREEZE_JUMP));
        return false;
    }
}
