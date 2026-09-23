package org.allaymc.api.eventbus.event.player;

import lombok.Getter;
import org.allaymc.api.annotation.CallerThread;
import org.allaymc.api.annotation.ThreadType;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.event.CancellableEvent;

/**
 * Called when a player starts or stops flying.
 *
 * <p>GearsMC fork: olay yukarı akışta hiç tetiklenmiyordu. Artık istemci uçmaya başladığını ya da
 * bıraktığını bildirdiğinde ({@code PlayerAuthInput} START/STOP_FLYING, {@code RequestAbility}),
 * durum değişmeden <b>önce</b> tetiklenir ve iptal edilebilir; iptalde sunucu durumu korunur ve
 * istemcinin yetenekleri yeniden gönderilir. Eklentinin kendi {@code setFlying} çağrısı olayı
 * tetiklemez. Böylece ada uçuş koruması uçuşu bir tik sonra kesmek yerine hiç başlatmaz, görünmez
 * yetkilinin uçuşu da kapanmaz.</p>
 *
 * @author daoge_cmd
 */
@Getter
@CallerThread(ThreadType.WORLD)
public class PlayerToggleFlightEvent extends PlayerEvent implements CancellableEvent {
    /**
     * Whether flight is enabled.
     */
    protected boolean value;

    public PlayerToggleFlightEvent(EntityPlayer player, boolean value) {
        super(player);
        this.value = value;
    }
}
