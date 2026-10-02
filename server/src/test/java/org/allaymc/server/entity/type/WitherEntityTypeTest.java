package org.allaymc.server.entity.type;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.testutils.AllayTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wither olumsuz bileseni kullanir ama zirh envanteri yoktur; bu yuzden olusturulurken
 * bilesen bagimliligi cozulemeyip cokuyordu ("Cannot find dependency EntityContainerHolderComponent").
 */
@ExtendWith(AllayTestExtension.class)
class WitherEntityTypeTest {

    @Test
    void witherCanBeCreatedWithoutArmorContainer() {
        var wither = assertDoesNotThrow(() -> EntityTypes.WITHER.createEntity(EntityInitInfo.builder().build()));
        assertNotNull(wither);
    }

    @Test
    void witherIsNotIgnitedBySunlight() {
        var wither = EntityTypes.WITHER.createEntity(EntityInitInfo.builder().build());
        assertFalse(wither.ignitedBySunlight());
    }

    @Test
    void otherUndeadStillIgniteBySunlight() {
        var zombie = EntityTypes.ZOMBIE.createEntity(EntityInitInfo.builder().build());
        assertTrue(zombie.ignitedBySunlight());
    }
}
