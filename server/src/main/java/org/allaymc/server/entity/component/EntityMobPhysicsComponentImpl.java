package org.allaymc.server.entity.component;

/**
 * Ne insansi ne de ureyebilen hayvan olan, yuruyen moblar icin fizik bileseni.
 *
 * <p>Tek degistirdigi sey adim yuksekligi: varsayilan {@code 0.0}, ve bu deger yol bulan bir
 * mobu onune cikan ilk yarim blokta ya da tek bloklik basamakta takili birakir.</p>
 */
public class EntityMobPhysicsComponentImpl extends EntityPhysicsComponentImpl {

    @Override
    public double getStepHeight() {
        return 0.6;
    }
}
