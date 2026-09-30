package org.allaymc.api.entity.interfaces;

import org.allaymc.api.entity.component.EntityPhysicsComponent;

public interface EntityVillagerV2 extends EntityLiving, EntityPhysicsComponent {
    /**
     * Meslek görünümü (istemciye VARIANT olarak gider). 0 işsiz köylüdür.
     */
    int getProfession();

    /**
     * Meslek görünümünü değiştirir ve izleyicilere bildirir.
     *
     * @param profession Bedrock meslek numarası (ör. kasap için 4)
     */
    void setProfession(int profession);
}
