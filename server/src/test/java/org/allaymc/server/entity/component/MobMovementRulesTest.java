package org.allaymc.server.entity.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobMovementRulesTest {

    @Test
    void spiderClimbsOnlyWhenPushingAgainstAWall() {
        // Ileri itiyor (0.15 blok/tick) ama hareket sifirlandi: tirman.
        assertTrue(EntitySpiderPhysicsComponentImpl.shouldClimb(0.15 * 0.15, 0.0));
        // Serbestce yuruyor: tirmanma.
        assertFalse(EntitySpiderPhysicsComponentImpl.shouldClimb(0.15 * 0.15, 0.15 * 0.15));
        // Durgun: itme yok, tirmanma.
        assertFalse(EntitySpiderPhysicsComponentImpl.shouldClimb(0.0, 0.0));
    }

    @Test
    void spiderClimbSpeedDependsOnGround() {
        assertEquals(0.32, EntitySpiderPhysicsComponentImpl.climbMotionY(true, 0.0), 1e-9);
        assertEquals(0.15, EntitySpiderPhysicsComponentImpl.climbMotionY(false, -0.3), 1e-9);
        // Zaten daha hizli yukseliyorsa hizi dusurme.
        assertEquals(0.5, EntitySpiderPhysicsComponentImpl.climbMotionY(true, 0.5), 1e-9);
    }

    @Test
    void rabbitVariantsAreValid() {
        for (int i = 0; i < 5000; i++) {
            int variant = EntityRabbitBaseComponentImpl.rollVariant();
            assertTrue(EntityRabbitBaseComponentImpl.validVariant(variant), "gecersiz cesit " + variant);
        }
        assertFalse(EntityRabbitBaseComponentImpl.validVariant(6));
        assertFalse(EntityRabbitBaseComponentImpl.validVariant(-1));
        assertTrue(EntityRabbitBaseComponentImpl.validVariant(99));
    }
}
