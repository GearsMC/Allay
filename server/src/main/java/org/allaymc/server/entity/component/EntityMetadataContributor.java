package org.allaymc.server.entity.component;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;

public interface EntityMetadataContributor {

    void writeMetadata(EntityDataMap metadata);
}
