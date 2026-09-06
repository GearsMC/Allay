package org.allaymc.server.container;

import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.server.container.impl.BaseContainer;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Slot etiketi olmayan kap kayitlarinin okunabildigini dogrular.
 *
 * <p>Vanilla Bedrock sabit boyutlu kaplarda ({@code ChiseledBookshelf}) Slot
 * etiketi yazmaz; sira listedeki konumdan gelir ve dolu olmayan raflar bos
 * compound olarak kaydedilir. Etiketi sart kosan okuma bu esyalari atliyor,
 * chunk bir sonraki kayitta yeniden yazilirken de <b>kalici olarak
 * dusuruyordu</b>. Hub dunyasindaki 11 kitaplikta bir yazili kitap tam olarak
 * boyle kayboluyordu.</p>
 */
@ExtendWith(AllayTestExtension.class)
class BaseContainerLoadNBTTest {

    private static NbtMap book(String name) {
        return NbtMap.builder().putString("Name", name).putByte("Count", (byte) 1).build();
    }

    @Test
    void slotlessEntriesFallBackToTheirListIndex() {
        var container = new BaseContainer(ContainerTypes.CHISELED_BOOKSHELF);

        // Bedrock'un yazdigi bicim: alti raf, ucuncusu dolu, Slot etiketi yok.
        container.loadNBT(List.of(NbtMap.EMPTY, NbtMap.EMPTY, book("minecraft:written_book"),
                NbtMap.EMPTY, NbtMap.EMPTY, NbtMap.EMPTY));

        assertEquals("minecraft:written_book",
                container.getItemStack(2).getItemType().getIdentifier().toString());
        assertSame(ItemAirStack.AIR_STACK, container.getItemStack(0));
        assertSame(ItemAirStack.AIR_STACK, container.getItemStack(5));
    }

    @Test
    void anExplicitSlotStillWins() {
        var container = new BaseContainer(ContainerTypes.CHISELED_BOOKSHELF);

        container.loadNBT(List.of(book("minecraft:book").toBuilder().putByte("Slot", (byte) 4).build()));

        assertSame(ItemAirStack.AIR_STACK, container.getItemStack(0));
        assertEquals("minecraft:book",
                container.getItemStack(4).getItemType().getIdentifier().toString());
    }

    @Test
    void anOutOfRangeSlotIsSkippedInsteadOfCrashing() {
        var container = new BaseContainer(ContainerTypes.CHISELED_BOOKSHELF);

        container.loadNBT(List.of(book("minecraft:book").toBuilder().putByte("Slot", (byte) 40).build()));

        for (int slot = 0; slot < 6; slot++) {
            assertSame(ItemAirStack.AIR_STACK, container.getItemStack(slot));
        }
    }
}
