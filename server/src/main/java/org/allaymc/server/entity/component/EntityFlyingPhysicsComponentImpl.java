package org.allaymc.server.entity.component;

/**
 * Blaze gibi kendi gucuyle havada duran moblar icin fizik.
 *
 * <p>Yercekimi tamamen kapali: havada duran bir mob yuksekligini kaldirma ile agirligin
 * dengesinden degil, izledigi rotadan aliyor. Hava surtunmesi varsayilanin belirgin sekilde
 * uzerine cikarildi ki {@code FlyController} itmeyi biraktigi anda hareket cabucak sonsun;
 * varsayilan surtunmeyle mob hedef noktasini fena halde asar ve gozle gorulur sekilde
 * sallanirdi.</p>
 */
public class EntityFlyingPhysicsComponentImpl extends EntityPhysicsComponentImpl {

    @Override
    public double getGravity() {
        return 0;
    }

    @Override
    public double getDragFactorInAir() {
        return 0.09;
    }

    @Override
    public boolean computeLiquidPhysics() {
        // Ucan bir mob sivi icinde saga sola sallanmamali; nereye gidecegine zaten rotasi karar veriyor.
        return false;
    }
}
