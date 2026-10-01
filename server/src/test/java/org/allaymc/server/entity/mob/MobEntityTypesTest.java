package org.allaymc.server.entity.mob;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.type.EntityType;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.world.Dimension;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HeartCore'da yapay zekasi olan ama motorda bos kayitli olan moblarin artik canli ve zeki oldugunu dogrular.
 */
@ExtendWith(AllayTestExtension.class)
class MobEntityTypesTest {

    @Test
    void formerlyEmptyMobsAreLivingAndIntelligent() {
        Dimension dimension = Mockito.mock(Dimension.class);
        for (EntityType<?> type : new EntityType<?>[]{
                EntityTypes.SPIDER, EntityTypes.SILVERFISH, EntityTypes.RABBIT,
                EntityTypes.PHANTOM, EntityTypes.WITHER_SKELETON}) {
            var entity = type.createEntity(EntityInitInfo.builder().pos(0, 1, 2).dimension(dimension).build());
            String id = type.getIdentifier().toString();
            assertInstanceOf(EntityLivingComponent.class, entity, id);
            assertInstanceOf(EntityAIComponent.class, entity, id);
            assertTrue(((EntityLivingComponent) entity).getMaxHealth() > 0, id);
        }
    }
}
