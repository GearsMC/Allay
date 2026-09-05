package org.allaymc.server.blockentity.component;

import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Mob doguranin blok varligi.
 *
 * <p>Yalnizca ayarlari saklar; dogurma davranisi henuz yok. Ayarlarin
 * saklanmasi bile onemli: kayitli olmayan bir blok varligi chunk yeniden
 * yazilirken tumden dusuyor ve doguranin hangi mobu, hangi araliklarla
 * dogurdugu kaliciyla kayboluyordu.</p>
 *
 * @see <a href="https://minecraft.wiki/w/Bedrock_Edition_level_format/Block_entity_format#Monster_Spawner">Monster Spawner</a>
 */
public class BlockEntityMobSpawnerBaseComponentImpl extends BlockEntityBaseComponentImpl {

    protected static final String TAG_ENTITY_IDENTIFIER = "EntityIdentifier";
    protected static final String TAG_DELAY = "Delay";
    protected static final String TAG_MIN_SPAWN_DELAY = "MinSpawnDelay";
    protected static final String TAG_MAX_SPAWN_DELAY = "MaxSpawnDelay";
    protected static final String TAG_SPAWN_COUNT = "SpawnCount";
    protected static final String TAG_MAX_NEARBY_ENTITIES = "MaxNearbyEntities";
    protected static final String TAG_REQUIRED_PLAYER_RANGE = "RequiredPlayerRange";
    protected static final String TAG_SPAWN_RANGE = "SpawnRange";
    protected static final String TAG_DISPLAY_ENTITY_WIDTH = "DisplayEntityWidth";
    protected static final String TAG_DISPLAY_ENTITY_HEIGHT = "DisplayEntityHeight";
    protected static final String TAG_DISPLAY_ENTITY_SCALE = "DisplayEntityScale";

    protected String entityIdentifier = "";
    protected short delay = 0;
    protected short minSpawnDelay = 200;
    protected short maxSpawnDelay = 800;
    protected short spawnCount = 4;
    protected short maxNearbyEntities = 6;
    protected short requiredPlayerRange = 16;
    protected short spawnRange = 4;
    protected float displayEntityWidth = 1.0f;
    protected float displayEntityHeight = 1.0f;
    protected float displayEntityScale = 1.0f;

    public BlockEntityMobSpawnerBaseComponentImpl(BlockEntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public NbtMap saveNBT() {
        return super.saveNBT().toBuilder()
                .putString(TAG_ENTITY_IDENTIFIER, entityIdentifier)
                .putShort(TAG_DELAY, delay)
                .putShort(TAG_MIN_SPAWN_DELAY, minSpawnDelay)
                .putShort(TAG_MAX_SPAWN_DELAY, maxSpawnDelay)
                .putShort(TAG_SPAWN_COUNT, spawnCount)
                .putShort(TAG_MAX_NEARBY_ENTITIES, maxNearbyEntities)
                .putShort(TAG_REQUIRED_PLAYER_RANGE, requiredPlayerRange)
                .putShort(TAG_SPAWN_RANGE, spawnRange)
                .putFloat(TAG_DISPLAY_ENTITY_WIDTH, displayEntityWidth)
                .putFloat(TAG_DISPLAY_ENTITY_HEIGHT, displayEntityHeight)
                .putFloat(TAG_DISPLAY_ENTITY_SCALE, displayEntityScale)
                .build();
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForString(TAG_ENTITY_IDENTIFIER, value -> this.entityIdentifier = value);
        nbt.listenForShort(TAG_DELAY, value -> this.delay = value);
        nbt.listenForShort(TAG_MIN_SPAWN_DELAY, value -> this.minSpawnDelay = value);
        nbt.listenForShort(TAG_MAX_SPAWN_DELAY, value -> this.maxSpawnDelay = value);
        nbt.listenForShort(TAG_SPAWN_COUNT, value -> this.spawnCount = value);
        nbt.listenForShort(TAG_MAX_NEARBY_ENTITIES, value -> this.maxNearbyEntities = value);
        nbt.listenForShort(TAG_REQUIRED_PLAYER_RANGE, value -> this.requiredPlayerRange = value);
        nbt.listenForShort(TAG_SPAWN_RANGE, value -> this.spawnRange = value);
        nbt.listenForFloat(TAG_DISPLAY_ENTITY_WIDTH, value -> this.displayEntityWidth = value);
        nbt.listenForFloat(TAG_DISPLAY_ENTITY_HEIGHT, value -> this.displayEntityHeight = value);
        nbt.listenForFloat(TAG_DISPLAY_ENTITY_SCALE, value -> this.displayEntityScale = value);
    }
}
