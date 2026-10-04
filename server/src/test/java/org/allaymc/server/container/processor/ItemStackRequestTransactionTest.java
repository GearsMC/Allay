package org.allaymc.server.container.processor;

import com.google.common.collect.BiMap;
import org.allaymc.api.container.ContainerViewer;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;
import org.allaymc.server.container.impl.BaseContainer;
import org.allaymc.server.container.impl.ChestContainerImpl;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ConsumeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftCreativeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.allaymc.api.item.interfaces.ItemAirStack.AIR_STACK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Reddedilen istekte yuvalar geri alınır ve istemci gerçek içerikle eşitlenir; başarılı istekte
 * değişiklik aynı konteynere bakan diğer oyunculara gider. Eskiden reddedilen istek istemcide
 * hayalet eşya bırakıyor, ortak sandıkta öteki oyuncu konan eşyayı görmüyordu.
 */
@ExtendWith(AllayTestExtension.class)
class ItemStackRequestTransactionTest {

    private Player player;
    private ChestContainerImpl chest;
    private ItemStackRequestTransaction transaction;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        when(player.isConnected()).thenReturn(true);
        chest = new ChestContainerImpl();
        transaction = ItemStackRequestTransaction.begin(player);
    }

    @AfterEach
    void tearDown() {
        transaction.close();
    }

    @SuppressWarnings("unchecked")
    private static void addViewer(BaseContainer container, byte id, ContainerViewer viewer) throws ReflectiveOperationException {
        var field = BaseContainer.class.getDeclaredField("viewers");
        field.setAccessible(true);
        ((BiMap<Byte, ContainerViewer>) field.get(container)).put(id, viewer);
    }

    private static ItemStack sword() {
        return ItemTypes.DIAMOND_SWORD.createItemStack(1);
    }

    /** Yuva 0'daki eşyayı yuva 1'e taşır (işlemcinin yaptığı gibi, göndermeden). */
    private void move(int from, int to) {
        var item = chest.getItemStack(from);
        chest.setItemStack(from, AIR_STACK, false);
        chest.setItemStack(to, item, false);
    }

    @Test
    void rollbackRestoresOriginalItemsWithSameStackIds() {
        var original = sword();
        original.setCustomName("Hediye");
        chest.setItemStack(0, original, false);

        transaction.capture(chest, 0);
        transaction.capture(chest, 1);
        move(0, 1);

        assertTrue(transaction.rollback());
        var restored = chest.getItemStack(0);
        assertEquals(ItemTypes.DIAMOND_SWORD, restored.getItemType());
        assertEquals("Hediye", restored.getCustomName());
        assertEquals(original.getUniqueId(), restored.getUniqueId());
        assertSame(AIR_STACK, chest.getItemStack(1));
    }

    @Test
    void firstSnapshotWinsWhenSlotIsCapturedAgain() {
        var stack = ItemTypes.DIAMOND.createItemStack(10);
        chest.setItemStack(0, stack, false);
        transaction.capture(chest, 0);
        stack.setCount(3);
        transaction.capture(chest, 0);

        transaction.rollback();
        assertEquals(10, chest.getItemStack(0).getCount());
    }

    @Test
    void irreversibleSideEffectSkipsRestore() {
        chest.setItemStack(0, sword(), false);
        transaction.capture(chest, 0);
        chest.setItemStack(0, AIR_STACK, false);
        // Örn. eşya yere atıldı: geri almak kopyalamak olurdu.
        ItemStackRequestTransaction.markIrreversible();

        assertTrue(transaction.isIrreversible());
        assertFalse(transaction.rollback());
        assertSame(AIR_STACK, chest.getItemStack(0));
    }

    @Test
    void markIrreversibleOutsideTransactionIsIgnored() {
        transaction.close();
        assertDoesNotThrow(ItemStackRequestTransaction::markIrreversible);
        assertFalse(transaction.isIrreversible());
    }

    @Test
    void commitSendsChangedSlotsToOtherViewersOnly() throws ReflectiveOperationException {
        var other = mock(ContainerViewer.class);
        addViewer(chest, (byte) 1, player);
        addViewer(chest, (byte) 2, other);

        transaction.capture(chest, 4);
        chest.setItemStack(4, sword(), false);
        transaction.commit();

        verify(other).viewContainerSlot(chest, 4);
        verify(player, never()).viewContainerSlot(any(), anyInt());
    }

    @Test
    void rejectedRequestResyncsPlayerWithServerContents() throws ReflectiveOperationException {
        var other = mock(ContainerViewer.class);
        addViewer(chest, (byte) 2, other);
        chest.setItemStack(2, sword(), false);
        transaction.capture(chest, 2);

        transaction.rollback();
        // Zamanlanan eşitleme (bir tick sonra) bunu çağırır.
        transaction.resync(List.of(new ItemStackRequestTransaction.SlotKey(chest, 2)));

        verify(player).viewContainerSlot(chest, 2);
        verify(other).viewContainerSlot(chest, 2);
    }

    @Test
    void invalidSlotIsIgnored() {
        assertDoesNotThrow(() -> transaction.capture(chest, 99));
        assertDoesNotThrow(() -> transaction.capture(chest, -1));
        assertTrue(transaction.rollback());
    }

    @Test
    void referencedSlotsCoverSlotActions() {
        var a = slot(ContainerSlotType.HOTBAR_AND_INVENTORY, 1);
        var b = slot(ContainerSlotType.LEVEL_ENTITY, 2);

        assertEquals(List.of(a, b), ItemStackRequestTransaction.referencedSlots(new PlaceAction(1, a, b)));
        assertEquals(List.of(a, b), ItemStackRequestTransaction.referencedSlots(new SwapAction(a, b)));
        assertEquals(List.of(a), ItemStackRequestTransaction.referencedSlots(new DropAction(1, a, false)));
        assertEquals(List.of(a), ItemStackRequestTransaction.referencedSlots(new ConsumeAction(1, a)));
        assertTrue(ItemStackRequestTransaction.referencedSlots(new CraftCreativeAction(1, 1)).isEmpty());
    }

    @Test
    void stacksWithDifferentDataDoNotMerge() {
        var plain = ItemTypes.DIAMOND.createItemStack(1);
        var named = ItemTypes.DIAMOND.createItemStack(1);
        named.setCustomName("Özel");
        // TransferItemActionProcessor artık türe değil buna bakıyor.
        assertFalse(named.canMerge(plain, true));
        assertTrue(plain.canMerge(ItemTypes.DIAMOND.createItemStack(5), true));
    }

    private static ItemStackRequestSlotData slot(ContainerSlotType type, int slot) {
        return new ItemStackRequestSlotData(type, slot, 0, new FullContainerName(type, null));
    }
}
