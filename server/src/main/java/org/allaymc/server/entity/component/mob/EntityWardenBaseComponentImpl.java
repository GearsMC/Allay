package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.ai.memory.MemoryTypes;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.player.GameMode;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.server.component.annotation.Dependency;
import org.allaymc.server.entity.ai.executor.EntityControlHelper;
import org.allaymc.server.entity.component.event.CEntityLoadNBTEvent;
import org.allaymc.server.entity.component.event.CEntitySaveNBTEvent;
import org.allaymc.server.entity.component.event.CEntityTickEvent;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class EntityWardenBaseComponentImpl extends EntityMobBaseComponentImpl {

    public static final int ANGRY_THRESHOLD = 80;
    public static final int MAX_ANGER = 150;
    public static final int HIT_ANGER = 100;
    public static final int SNIFF_ANGER = 35;
    public static final int VIBRATION_ANGER = 10;

    protected static final double VIBRATION_RANGE = 16;
    protected static final double SNIFF_RANGE = 24;
    protected static final double DARKNESS_RANGE = 20;
    protected static final int DARKNESS_INTERVAL = 120;
    protected static final int DARKNESS_DURATION = 260;
    protected static final int SNIFF_TICKS = 84;
    protected static final String TAG_ANGER = "Anger";
    protected static final String TAG_ANGER_AMOUNT = "Amount";
    protected static final String TAG_UUID_MOST = "UUIDMost";
    protected static final String TAG_UUID_LEAST = "UUIDLeast";
    protected static final String TAG_SONIC_COOLDOWN = "SonicCooldown";

    @Dependency
    protected EntityAIComponent aiComponent;

    protected final Map<Long, Integer> anger = new ConcurrentHashMap<>();
    protected final Map<UUID, Integer> pendingAnger = new ConcurrentHashMap<>();
    protected final Map<Long, Vector3d> lastPositions = new HashMap<>();

    protected volatile boolean sonicCharging;
    protected volatile long nextSonicBoomTick;
    protected volatile boolean sniffing;
    protected int sniffTicks;
    protected int nextSniffTick = 100;
    protected int heartbeatDelay = 40;
    protected boolean broadcastSonic;
    protected boolean broadcastSniffing;

    public EntityWardenBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo, 0.9, 2.9);
    }

    public void addAnger(Entity entity, int amount) {
        if (!(entity instanceof EntityPlayer player) || !isValidAngerTarget(player)) {
            return;
        }

        anger.merge(player.getRuntimeId(), amount, (old, added) -> Math.min(MAX_ANGER, old + added));
    }

    public int getAnger(Entity entity) {
        return anger.getOrDefault(entity.getRuntimeId(), 0);
    }

    public void setSonicCharging(boolean sonicCharging) {
        this.sonicCharging = sonicCharging;
    }

    public long getNextSonicBoomTick() {
        return nextSonicBoomTick;
    }

    public void setNextSonicBoomTick(long nextSonicBoomTick) {
        this.nextSonicBoomTick = nextSonicBoomTick;
    }

    protected boolean isValidAngerTarget(EntityPlayer player) {
        return player.isAlive() && player.getDimension() == getDimension()
               && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
               && EntityControlHelper.allowsTarget(thisEntity, player);
    }

    @EventHandler
    protected void onWardenTick(CEntityTickEvent event) {
        if (!thisEntity.isAlive()) {
            return;
        }

        resolvePendingAnger();
        var tick = event.getCurrentTick();
        if (tick % 20 == 0) {
            senseVibrations();
            decayAnger();
            updateTarget();
        }
        if (tick % DARKNESS_INTERVAL == 0) {
            pulseDarkness();
        }
        tickSniff();

        var changed = false;
        if (sonicCharging != broadcastSonic) {
            broadcastSonic = sonicCharging;
            changed = true;
        }
        if (sniffing != broadcastSniffing) {
            broadcastSniffing = sniffing;
            changed = true;
        }
        var delay = computeHeartbeatDelay();
        if (delay != heartbeatDelay) {
            heartbeatDelay = delay;
            changed = true;
        }
        if (changed) {
            broadcastState();
        }
    }

    /**
     * Anger is keyed by runtime id while the warden is loaded, but runtime ids change after a
     * chunk reload. Suspects are stored by player UUID until that player is in the dimension again.
     */
    protected void resolvePendingAnger() {
        if (pendingAnger.isEmpty() || getDimension() == null) {
            return;
        }

        var resolved = false;
        var iterator = pendingAnger.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var player = findPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            anger.merge(player.getRuntimeId(), entry.getValue(), (old, added) -> Math.min(MAX_ANGER, old + added));
            iterator.remove();
            resolved = true;
        }
        if (resolved) {
            updateTarget();
        }
    }

    protected EntityPlayer findPlayer(UUID uniqueId) {
        for (var controller : getDimension().getPlayers()) {
            var player = controller.getControlledEntity();
            if (player != null && uniqueId.equals(player.getUniqueId())) {
                return player;
            }
        }
        return null;
    }

    @EventHandler
    protected void onWardenLoadNBT(CEntityLoadNBTEvent event) {
        var nbt = event.getNbt();
        nbt.listenForList(TAG_ANGER, NbtType.COMPOUND, entries -> {
            for (var entry : entries) {
                if (!entry.containsKey(TAG_UUID_MOST) || !entry.containsKey(TAG_UUID_LEAST)) {
                    continue;
                }
                var amount = entry.getInt(TAG_ANGER_AMOUNT, 0);
                if (amount <= 0) {
                    continue;
                }
                pendingAnger.put(new UUID(entry.getLong(TAG_UUID_MOST), entry.getLong(TAG_UUID_LEAST)), Math.min(MAX_ANGER, amount));
            }
        });
        var cooldown = nbt.getInt(TAG_SONIC_COOLDOWN, 0);
        if (cooldown > 0) {
            nextSonicBoomTick = thisEntity.getTick() + cooldown;
        }
    }

    @EventHandler
    protected void onWardenSaveNBT(CEntitySaveNBTEvent event) {
        var saved = new ArrayList<NbtMap>();
        var seen = new HashMap<UUID, Integer>();
        if (getDimension() != null) {
            var manager = getDimension().getEntityManager();
            for (var entry : anger.entrySet()) {
                if (!(manager.getEntity(entry.getKey()) instanceof EntityPlayer player) || entry.getValue() <= 0) {
                    continue;
                }
                var uniqueId = player.getUniqueId();
                if (uniqueId != null) {
                    seen.put(uniqueId, entry.getValue());
                }
            }
        }
        pendingAnger.forEach(seen::putIfAbsent);
        for (var entry : seen.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            saved.add(NbtMap.builder()
                    .putLong(TAG_UUID_MOST, entry.getKey().getMostSignificantBits())
                    .putLong(TAG_UUID_LEAST, entry.getKey().getLeastSignificantBits())
                    .putInt(TAG_ANGER_AMOUNT, entry.getValue())
                    .build());
        }
        if (!saved.isEmpty()) {
            event.getNbt().putList(TAG_ANGER, NbtType.COMPOUND, saved);
        }
        var remaining = nextSonicBoomTick - thisEntity.getTick();
        if (remaining > 0) {
            event.getNbt().putInt(TAG_SONIC_COOLDOWN, (int) remaining);
        }
    }

    protected void senseVibrations() {
        var rangeSquared = VIBRATION_RANGE * VIBRATION_RANGE;
        var seen = new HashMap<Long, Vector3d>();
        for (var controller : getDimension().getPlayers()) {
            var player = controller.getControlledEntity();
            if (player == null || !isValidAngerTarget(player) || player.getLocation().distanceSquared(location) > rangeSquared) {
                continue;
            }

            var loc = player.getLocation();
            var current = new Vector3d(loc.x(), loc.y(), loc.z());
            seen.put(player.getRuntimeId(), current);
            var previous = lastPositions.get(player.getRuntimeId());
            if (previous != null && !player.isSneaking() && previous.distanceSquared(current) > 0.01) {
                addAnger(player, VIBRATION_ANGER);
            }
        }
        lastPositions.clear();
        lastPositions.putAll(seen);
    }

    protected void decayAnger() {
        var manager = getDimension().getEntityManager();
        anger.entrySet().removeIf(entry -> {
            var entity = manager.getEntity(entry.getKey());
            if (!(entity instanceof EntityPlayer player) || !isValidAngerTarget(player)) {
                return true;
            }
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    protected void updateTarget() {
        Long best = null;
        var bestAnger = ANGRY_THRESHOLD - 1;
        for (var entry : anger.entrySet()) {
            if (entry.getValue() > bestAnger) {
                bestAnger = entry.getValue();
                best = entry.getKey();
            }
        }

        var memory = aiComponent.getMemoryStorage();
        var current = memory.get(MemoryTypes.ATTACK_TARGET);
        if (best == null) {
            if (current != null) {
                memory.clear(MemoryTypes.ATTACK_TARGET);
            }
            return;
        }

        if (current == null) {
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_WARDEN_ROAR));
        }
        memory.put(MemoryTypes.ATTACK_TARGET, best);
    }

    protected void pulseDarkness() {
        var rangeSquared = DARKNESS_RANGE * DARKNESS_RANGE;
        for (var controller : getDimension().getPlayers()) {
            var player = controller.getControlledEntity();
            if (player == null || !player.isAlive() || player.getLocation().distanceSquared(location) > rangeSquared) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }
            player.addEffect(new EffectInstance(EffectTypes.DARKNESS, 0, DARKNESS_DURATION, false, true));
        }
    }

    protected void tickSniff() {
        if (sniffing) {
            if (--sniffTicks <= 0) {
                sniffing = false;
                sniffNearestPlayer();
            }
            return;
        }

        if (aiComponent.getMemoryStorage().get(MemoryTypes.ATTACK_TARGET) != null) {
            return;
        }
        if (--nextSniffTick <= 0) {
            sniffing = true;
            sniffTicks = SNIFF_TICKS;
            nextSniffTick = ThreadLocalRandom.current().nextInt(100, 201);
            getDimension().addSound(location, new CustomSound(SoundNames.MOB_WARDEN_SNIFF));
        }
    }

    protected void sniffNearestPlayer() {
        EntityPlayer nearest = null;
        var nearestDistance = SNIFF_RANGE * SNIFF_RANGE;
        for (var controller : getDimension().getPlayers()) {
            var player = controller.getControlledEntity();
            if (player == null || !isValidAngerTarget(player)) {
                continue;
            }
            var distance = player.getLocation().distanceSquared(location);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        if (nearest != null) {
            addAnger(nearest, SNIFF_ANGER);
        }
    }

    protected int computeHeartbeatDelay() {
        var max = 0;
        for (var value : anger.values()) {
            max = Math.max(max, value);
        }
        var ratio = Math.min(1f, max / (float) ANGRY_THRESHOLD);
        return 40 - (int) Math.floor(ratio * 30);
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        super.writeMetadata(metadata);
        metadata.setFlag(EntityFlag.FIRE_IMMUNE, true);
        metadata.setFlag(EntityFlag.SONIC_BOOM, sonicCharging);
        metadata.setFlag(EntityFlag.SNIFFING, sniffing);
        metadata.put(EntityDataTypes.HEARTBEAT_INTERVAL_TICKS, heartbeatDelay);
        metadata.put(EntityDataTypes.HEARTBEAT_SOUND_EVENT, SoundEvent.HEARTBEAT);
    }
}
