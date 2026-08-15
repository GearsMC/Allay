package org.allaymc.server.entity.component.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.allaymc.api.eventbus.event.Event;

/**
 * Bir kup mobunun boyutu belirlendiginde tetiklenir.
 *
 * <p>Boyut kupun butun olculerini suruklyor ama farkli bilesenlerde kullaniliyor: carpisma kutusu
 * temel bilesende, can canli varlik bileseninde. Boyut uc ayri anda belirlenebildigi icin — dogal
 * dogum, diskten yukleme, bolunme — bunlari birbirine baglamanin temiz yolu, degisimi tek bir
 * olayla duyurmak.</p>
 */
@Getter
@AllArgsConstructor
public class CEntityCubeSizeChangeEvent extends Event {
    protected int size;
}
