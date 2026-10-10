package org.allaymc.server.entity.component.mob;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.server.entity.component.EntityMetadataContributor;
import org.allaymc.server.entity.component.EntitySpiderBaseComponentImpl;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

public class EntityCaveSpiderBaseComponentImpl extends EntitySpiderBaseComponentImpl implements EntityMetadataContributor {

    public EntityCaveSpiderBaseComponentImpl(EntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public AABBdc getBaseAABB() {
        return new AABBd(-0.35, 0.0, -0.35, 0.35, 0.5, 0.35);
    }

    @Override
    public void writeMetadata(EntityDataMap metadata) {
        metadata.setFlag(EntityFlag.WALL_CLIMBING, isClimbing());
    }
}
