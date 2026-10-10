package org.allaymc.testutils;

import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.server.Server;
import org.allaymc.api.world.Dimension;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public final class MobTestSite {

    private static final AtomicInteger NEXT_SITE = new AtomicInteger();

    private final List<Entity> spawned = new ArrayList<>();
    private final Dimension dimension;
    private final Vector3i origin;

    private MobTestSite(Dimension dimension, Vector3i origin) {
        this.dimension = dimension;
        this.origin = origin;
    }

    public static MobTestSite create() {
        var spawn = Server.getInstance().getWorldPool().getGlobalSpawnPoint();
        var dimension = spawn.dimension();
        var site = NEXT_SITE.getAndIncrement();
        var origin = new Vector3i((int) spawn.x() - 300 - site * 40, (int) spawn.y() + 70, (int) spawn.z() + 300);
        for (var dx = -12; dx <= 24; dx++) {
            for (var dz = -12; dz <= 12; dz++) {
                var x = origin.x + dx;
                var z = origin.z + dz;
                dimension.getChunkManager().getOrLoadChunk(x >> 4, z >> 4).join();
                dimension.setBlockState(x, origin.y - 1, z, BlockTypes.STONE.getDefaultState());
                for (var dy = 0; dy <= 6; dy++) {
                    dimension.setBlockState(x, origin.y + dy, z, BlockTypes.AIR.getDefaultState());
                }
            }
        }
        return new MobTestSite(dimension, origin);
    }

    public Dimension dimension() {
        return dimension;
    }

    public Vector3d at(double dx, double dy, double dz) {
        return new Vector3d(origin.x + dx + 0.5, origin.y + dy, origin.z + dz + 0.5);
    }

    public <T extends Entity> T spawn(EntityType<T> type, double dx, double dz, boolean frozen) {
        var pos = at(dx, 0, dz);
        var entity = type.createEntity(EntityInitInfo.builder().dimension(dimension).pos(pos).build());
        if (frozen && entity instanceof EntityIntelligent intelligent) {
            intelligent.setManualControlEnabled(true);
        }
        spawned.add(entity);
        var added = new CompletableFuture<Void>();
        dimension.getEntityManager().addEntity(entity, () -> added.complete(null));
        added.orTimeout(5, TimeUnit.SECONDS).join();
        waitFor(entity::isAlive, 2000);
        if (!entity.isAlive()) {
            throw new IllegalStateException(type.getIdentifier() + " dunyaya eklenemedi");
        }
        return entity;
    }

    public void track(Entity entity) {
        spawned.add(entity);
    }

    public long count(Class<?> type, double range) {
        var center = at(0, 0, 0);
        return dimension.getEntities().values().stream()
                .filter(type::isInstance)
                .filter(entity -> entity.getLocation().distanceSquared(center) <= range * range)
                .count();
    }

    public <T> T findNear(Class<T> type, double range) {
        var center = at(0, 0, 0);
        return dimension.getEntities().values().stream()
                .filter(type::isInstance)
                .filter(entity -> entity.getLocation().distanceSquared(center) <= range * range)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    public void onWorldThread(Runnable action) {
        var done = new CompletableFuture<Void>();
        dimension.getScheduler().runLater(dimension, () -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable throwable) {
                done.completeExceptionally(throwable);
            }
        });
        done.orTimeout(5, TimeUnit.SECONDS).join();
    }

    public static boolean waitFor(BooleanSupplier condition, long timeoutMillis) {
        var deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return condition.getAsBoolean();
            }
        }
        return condition.getAsBoolean();
    }

    public void cleanUp() {
        for (var entity : spawned) {
            if (!entity.willBeDespawnedLater() && entity.isAlive()) {
                entity.remove();
            }
        }
        spawned.clear();
    }
}
