package org.allaymc.server.entity.ai.controller;

import org.allaymc.api.entity.ai.controller.Controller;
import org.allaymc.api.entity.component.EntitySulfurCubeBaseComponent;
import org.allaymc.api.entity.interfaces.EntityIntelligent;

/**
 * Kup moblari icin hareket kontrolcusu: yurumez, ziplar.
 *
 * <p>{@link WalkController} her tick yatay hiz vererek varligi akitir; kup ise yerde bekleyip
 * araliklarla sicrar ve havadayken yonunu degistiremez. Bu yuzden ayri bir kontrolcu gerekiyor —
 * yurume kontrolcusuyle kup, ziplama animasyonu oynarken kayarak ilerlermis gibi gorunurdu.</p>
 *
 * <p>Hedefe yonelme yalnizca sicrama aninda yapilir; sonrasi tamamen fizige kalir, ki kupun bir
 * top gibi savrulup sekmesini saglayan sey de budur.</p>
 */
public class CubeJumpController implements Controller {

    /**
     * Kucuk kupun buyuge gore hareket orani.
     *
     * <p>Wiki'nin hiz degerleri buyuk icin {@code 0.4}, kucuk icin {@code 0.3}; yani kucuk kup
     * buyugun dortte ucu kadar hareket ediyor. Sicrayis olculeri de bu oranla kucultuluyor, boylece
     * kucuk kup boyuna gore ayni yuksekligi ziplamis oluyor.</p>
     */
    protected static final double SMALL_FACTOR = 0.75;

    protected final float jumpSpeed;
    protected final float jumpHeight;
    protected final int jumpInterval;
    protected final int jumpAnimationTicks;

    protected int cooldown;

    /**
     * @param jumpSpeed buyuk kupun her sicrayista aldigi yatay hiz
     * @param jumpHeight buyuk kupun her sicrayista aldigi dikey hiz
     * @param jumpInterval iki sicrayis arasindaki bekleme (tick)
     * @param jumpAnimationTicks istemcinin ziplama animasyonunu kac tick oynatacagi
     */
    public CubeJumpController(float jumpSpeed, float jumpHeight, int jumpInterval, int jumpAnimationTicks) {
        this.jumpSpeed = jumpSpeed;
        this.jumpHeight = jumpHeight;
        this.jumpInterval = jumpInterval;
        this.jumpAnimationTicks = jumpAnimationTicks;
    }

    @Override
    public boolean control(EntityIntelligent entity) {
        if (cooldown > 0) {
            cooldown--;
        }

        // Blok emmis kup hic kipirdamaz: "When a sulfur cube has absorbed a block, it stops moving."
        var cube = entity instanceof EntitySulfurCubeBaseComponent c ? c : null;
        if (cube != null && cube.getAbsorbedBlock() != null) {
            return false;
        }

        if (!entity.hasMoveDirection()) {
            return false;
        }

        var end = entity.getMoveDirectionEnd();
        if (end == null) {
            return false;
        }

        // Havadayken yon degistirilemez; kup nereye sicradiysa oraya iner.
        if (!entity.isOnGround() || cooldown > 0) {
            return false;
        }

        var loc = entity.getLocation();
        double dx = end.x() - loc.x();
        double dz = end.z() - loc.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 0.001) {
            return false;
        }

        var scale = cube != null && !cube.isLarge() ? SMALL_FACTOR : 1.0;
        var horizontal = jumpSpeed * scale;
        entity.setMotion(dx / length * horizontal, jumpHeight * scale, dz / length * horizontal);
        cooldown = jumpInterval;

        if (cube != null) {
            cube.startJump(jumpAnimationTicks);
        }

        return true;
    }
}
