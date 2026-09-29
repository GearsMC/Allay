package org.allaymc.server.entity.component;

/**
 * Blaze gibi kendi gucuyle havada duran moblar icin fizik.
 *
 * <p>Yercekimi varsayilan olarak kapali: havada duran bir mob yuksekligini kaldirma ile
 * agirligin dengesinden degil, izledigi rotadan aliyor. Kapatma {@link #setHasGravity} bayragi
 * uzerinden yapiliyor, cekim degerinin kendisi sifirlanmiyor; boylece bir eklenti mobu
 * {@code setHasGravity(true)} ile yere indirebilir (orn. yapay zekasi dondurulmus bir spawner
 * blaze'i havada asili kalmak yerine duser). Hava surtunmesi varsayilanin belirgin sekilde
 * uzerine cikarildi ki {@code FlyController} itmeyi biraktigi anda hareket cabucak sonsun;
 * varsayilan surtunmeyle mob hedef noktasini fena halde asar ve gozle gorulur sekilde
 * sallanirdi.</p>
 */
public class EntityFlyingPhysicsComponentImpl extends EntityPhysicsComponentImpl {

    public EntityFlyingPhysicsComponentImpl() {
        super();
        this.hasGravity = false;
    }

    @Override
    public void setHasGravity(boolean hasGravity) {
        if (hasGravity && !this.hasGravity) {
            // Ucarken biriken inis mesafesi dusme hasarina donusmesin; dusus simdi basliyor.
            this.fallDistance = 0;
        }
        super.setHasGravity(hasGravity);
    }

    @Override
    public double getDragFactorInAir() {
        return 0.09;
    }

    @Override
    public boolean computeLiquidPhysics() {
        // Ucan bir mob sivi icinde saga sola sallanmamali; nereye gidecegine zaten rotasi karar veriyor.
        // Yercekimi acildiysa mob artik ucmuyor, diger moblar gibi sivida yuzer/batar.
        return hasGravity;
    }
}
