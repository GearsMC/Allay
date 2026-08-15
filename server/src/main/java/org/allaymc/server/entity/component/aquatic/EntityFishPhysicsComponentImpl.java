package org.allaymc.server.entity.component.aquatic;

import org.allaymc.server.entity.component.EntityAquaticPhysicsComponentImpl;

/**
 * Baliklarin fizigi.
 *
 * <p>Akselottan ayrilmasinin sebebi yercekimi. Resmi davranis paketinde dort balik turunun de
 * fizigi {@code "minecraft:physics": {"has_gravity": false}} seklinde tanimli: balik <em>hicbir
 * kosulda</em> dusmez, sudan cikarilinca bile. Bedrock'ta suyu altindan alinan bir baligin havada
 * asili kalip cirpinmasi bundan. Akselotta ise {@code "minecraft:physics": {}} yaziyor, yani normal
 * yercekimi; o karada yuruyebilen amfibi bir hayvan.</p>
 *
 * <p>Ayrica resmi tanimda baliklarin gezinme bileseni {@code "can_walk": false} diyor, bu yuzden
 * basamak payi da yok.</p>
 */
public class EntityFishPhysicsComponentImpl extends EntityAquaticPhysicsComponentImpl {

    @Override
    public double getGravity() {
        return 0;
    }

    @Override
    public double getStepHeight() {
        // Balik yuruyemez; asacak bir basamagi da yok.
        return 0;
    }
}
