package org.allaymc.server.entity.component.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.entity.data.SulfurCubeArchetype;
import org.allaymc.api.eventbus.event.Event;

/**
 * Bir sulfur kupunun boyutu ya da emdigi blok degistiginde tetiklenir.
 *
 * <p>Bu iki deger kupun her seyini suruklyor ama farkli bilesenlerde kullaniliyor: carpisma kutusu
 * temel bilesende, can ve hasar bagisikligi canli varlik bileseninde, hiz ve sekme fizikte.
 * Degisimi tek bir olayla duyurmak bunlari birbirine baglamanin temiz yolu.</p>
 */
@Getter
@AllArgsConstructor
public class CEntitySulfurCubeChangeEvent extends Event {
    protected boolean large;
    protected BlockState absorbedBlock;
    protected SulfurCubeArchetype archetype;
}
