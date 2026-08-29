package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityAIComponent;
import org.allaymc.api.entity.component.EntityAnimalComponent;
import org.allaymc.api.entity.component.EntityBabyComponent;
import org.allaymc.api.entity.component.EntityHeadYawComponent;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityParallelTickComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityAnimal;
import org.allaymc.api.entity.interfaces.EntityFox;
import org.allaymc.api.entity.interfaces.EntityIntelligent;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prevents the fox registration from falling back to the visual-only default entity type.
 */
@ExtendWith(AllayTestExtension.class)
class FoxEntityTypeTest {

    @Test
    void foxExposesEveryAnimalCapability() {
        var fox = createFox();

        assertInstanceOf(EntityAnimal.class, fox);
        assertInstanceOf(EntityIntelligent.class, fox);
        assertInstanceOf(EntityLivingComponent.class, fox);
        assertInstanceOf(EntityPhysicsComponent.class, fox);
        assertInstanceOf(EntityAIComponent.class, fox);
        assertInstanceOf(EntityParallelTickComponent.class, fox);
        assertInstanceOf(EntityAnimalComponent.class, fox);
        assertInstanceOf(EntityBabyComponent.class, fox);
        assertInstanceOf(EntityHeadYawComponent.class, fox);
    }

    @Test
    void foxHasVanillaHealthAndBreedingItems() {
        var fox = createFox();
        var living = assertInstanceOf(EntityLivingComponent.class, fox);
        var animal = assertInstanceOf(EntityAnimalComponent.class, fox);

        assertEquals(10, living.getMaxHealth());
        assertEquals(10, living.getHealth());
        assertTrue(animal.isBreedingItem(ItemTypes.SWEET_BERRIES.createItemStack()));
        assertTrue(animal.isBreedingItem(ItemTypes.GLOW_BERRIES.createItemStack()));
        assertFalse(animal.isBreedingItem(ItemTypes.WHEAT.createItemStack()));
    }

    @Test
    void foxHasAReadyNavigationPipeline() {
        var ai = assertInstanceOf(EntityAIComponent.class, createFox());

        assertNotNull(ai.getBehaviorGroup());
        assertNotNull(ai.getBehaviorGroup().getRouteFinder());
        assertFalse(ai.getBehaviorGroup().getControllers().isEmpty());
    }

    private static EntityFox createFox() {
        return EntityTypes.FOX.createEntity(EntityInitInfo.builder().build());
    }
}
