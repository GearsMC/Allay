package org.allaymc.server.entity.ai.executor;

import org.allaymc.api.entity.ai.memory.MemoryType;
import org.allaymc.api.entity.interfaces.EntityArrow;
import org.allaymc.api.item.data.PotionType;

public class TippedBowAttackExecutor extends BowAttackExecutor {

    protected final PotionType potionType;

    public TippedBowAttackExecutor(MemoryType<Long> targetIdMemory, float speed, double maxSenseRange,
                                   boolean clearTargetAfterLose, PotionType potionType) {
        super(targetIdMemory, speed, maxSenseRange, clearTargetAfterLose);
        this.potionType = potionType;
    }

    @Override
    protected void configureArrow(EntityArrow arrow) {
        arrow.setPotionType(potionType);
    }
}
