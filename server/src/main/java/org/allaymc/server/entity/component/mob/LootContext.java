package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public record LootContext(Entity entity, int looting, boolean killedByPlayer, boolean onFire, boolean baby) {

    public ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    public int count(int min, int max) {
        return random().nextInt(min, max + 1) + random().nextInt(looting + 1);
    }

    public boolean chance(double chance, double lootingMultiplier) {
        return random().nextDouble() < chance + lootingMultiplier * looting;
    }
}
