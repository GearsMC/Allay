package org.allaymc.server.container.processor;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.container.Container;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ConsumeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DestroyAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * GearsMC fork: tek bir ItemStackRequest isteğinin konteyner değişikliklerini bir işlem gibi yönetir.
 *
 * <ul>
 *   <li>Her eylemden önce, eylemin dokunacağı yuvaların ilk hali (eşya kimliğiyle) saklanır.</li>
 *   <li>İstek reddedilirse yuvalar ilk haline döner; istemci isteğin tamamını geri aldığı için
 *       sunucu da öyle yapar. Önceki bir eylem geri alınamaz bir yan etki yaptıysa (yere atma, XP
 *       harcama, takas kullanımı…) geri alma yapılmaz; aksi halde eşya kopyalanabilirdi.</li>
 *   <li>Reddedilen her istekten sonra istemciye ilgili yuvaların sunucudaki gerçek içeriği gönderilir.
 *       Eskiden yalnızca "hata" deniyordu; istemcinin görüntüsü zaten yanlışsa öyle kalıyor, oyuncu
 *       olmayan eşyayı görüyor ya da gördüğü eşya kayboluyordu.</li>
 *   <li>Başarılı istekte değişen yuvalar aynı konteynere bakan diğer oyunculara gönderilir. İşlemciler
 *       yuvaları göndermeden değiştirdiği için ortak sandıkta öteki oyuncu değişikliği görmüyordu.</li>
 *   <li>CREATED_OUTPUT yalnızca istek içi geçici alandır; her istek sonunda temizlenir.</li>
 * </ul>
 *
 * <p>İstek işlenirken geçerli işlem thread'e bağlıdır; işlemciler geri alınamaz bir yan etki yaptıklarında
 * {@link #markIrreversible()} çağırır.</p>
 */
@Slf4j
public final class ItemStackRequestTransaction {

    private static final ThreadLocal<ItemStackRequestTransaction> CURRENT = new ThreadLocal<>();

    private final Player player;
    private final Map<SlotKey, ItemStack> snapshots = new LinkedHashMap<>();
    private boolean irreversible;

    private ItemStackRequestTransaction(Player player) {
        this.player = player;
    }

    /**
     * Yeni bir işlem başlatır ve geçerli thread'e bağlar.
     *
     * @param player isteği gönderen oyuncu
     * @return işlem; iş bitince {@link #close()} çağrılmalı
     */
    public static ItemStackRequestTransaction begin(Player player) {
        var transaction = new ItemStackRequestTransaction(player);
        CURRENT.set(transaction);
        return transaction;
    }

    /**
     * Geçerli isteğin geri alınamaz bir yan etki yaptığını bildirir. İstek sonradan reddedilse de
     * yuvalar geri alınmaz, yalnızca istemci gerçek içerikle eşitlenir.
     */
    public static void markIrreversible() {
        var transaction = CURRENT.get();
        if (transaction != null) {
            transaction.irreversible = true;
        }
    }

    /**
     * Eylemin dokunacağı yuvaların ilk halini saklar. Eylem işlenmeden önce çağrılmalıdır.
     *
     * @param action işlenecek eylem
     */
    public void capture(ItemStackRequestAction action) {
        for (var slotData : referencedSlots(action)) {
            try {
                Container container = ContainerActionProcessor.getContainerFrom(player, slotData.containerName());
                if (container == null) {
                    continue;
                }
                var slot = ContainerActionProcessor.fromNetworkSlotIndexOrNull(container, slotData.slot());
                if (slot != null) {
                    capture(container, slot);
                }
            } catch (RuntimeException ignored) {
                // Çözülemeyen yuvayı işlemci de reddeder; saklanacak bir şey yok.
            }
        }
    }

    void capture(Container container, int slot) {
        if (slot < 0 || slot >= container.getContainerType().getSize()) {
            return;
        }
        snapshots.computeIfAbsent(new SlotKey(container, slot), key -> copyOf(container.getItemStack(slot)));
    }

    /**
     * @return saklanan yuvalardan biri geri alınamaz bir yan etkiden sonra mı
     */
    public boolean isIrreversible() {
        return irreversible;
    }

    /**
     * İstek başarılı: değişen yuvaları bu oyuncu dışındaki izleyicilere gönderir (istemcinin kendisi
     * sonucu yanıttan öğrenir).
     */
    public void commit() {
        for (var key : snapshots.keySet()) {
            sendToOtherViewers(key);
        }
    }

    /**
     * İstek reddedildi: mümkünse yuvaları ilk haline döndürür ve istemciyi bir tick sonra sunucudaki
     * gerçek içerikle eşitler. Gecikme, istemcinin hata yanıtıyla yaptığı kendi geri almasının
     * gönderdiğimiz yuvaların üzerine yazmaması içindir.
     *
     * @return yuvalar geri alındıysa {@code true}
     */
    public boolean rollback() {
        var restored = !irreversible;
        if (restored) {
            snapshots.forEach((key, item) -> key.container().setItemStack(key.slot(), item, false));
        }
        var keys = List.copyOf(snapshots.keySet());
        for (var key : keys) {
            sendToOtherViewers(key);
        }
        scheduleResync(keys);
        return restored;
    }

    /**
     * İşlemi bitirir: CREATED_OUTPUT'u temizler ve thread bağını kaldırır.
     */
    public void close() {
        try {
            var entity = player.getControlledEntity();
            if (entity != null) {
                var createdOutput = entity.getContainer(ContainerTypes.CREATED_OUTPUT);
                if (createdOutput != null) {
                    createdOutput.clearSlot(0, false);
                }
            }
        } finally {
            if (CURRENT.get() == this) {
                CURRENT.remove();
            }
        }
    }

    /**
     * Saklanan yuvaların sunucudaki şu anki içeriği; hata kayıtları için.
     */
    public String describeSlots() {
        var joiner = new StringJoiner(", ", "[", "]");
        snapshots.keySet().forEach(key -> {
            var item = key.container().getItemStack(key.slot());
            joiner.add(key.container().getContainerType() + "#" + key.slot() + "=" + describe(item));
        });
        return joiner.toString();
    }

    static String describe(ItemStack item) {
        if (item == null || item.getItemType() == ItemTypes.AIR) {
            return "air";
        }
        return item.getItemType().getIdentifier() + "x" + item.getCount();
    }

    static List<ItemStackRequestSlotData> referencedSlots(ItemStackRequestAction action) {
        var slots = new ArrayList<ItemStackRequestSlotData>(2);
        switch (action) {
            case TransferItemStackRequestAction transfer -> {
                slots.add(transfer.source());
                slots.add(transfer.destination());
            }
            case SwapAction swap -> {
                slots.add(swap.source());
                slots.add(swap.destination());
            }
            case DropAction drop -> slots.add(drop.source());
            case DestroyAction destroy -> slots.add(destroy.source());
            case ConsumeAction consume -> slots.add(consume.source());
            default -> {
            }
        }
        slots.removeIf(slot -> slot == null || slot.containerName() == null);
        return slots;
    }

    private void sendToOtherViewers(SlotKey key) {
        if (key.container().getContainerType() == ContainerTypes.CREATED_OUTPUT) {
            return;
        }
        for (var viewer : List.copyOf(key.container().getViewers().values())) {
            if (viewer == player) {
                continue;
            }
            try {
                viewer.viewContainerSlot(key.container(), key.slot());
            } catch (IllegalStateException closed) {
                // İzleyici bu arada kapattı.
            }
        }
    }

    private void scheduleResync(List<SlotKey> keys) {
        var entity = player.getControlledEntity();
        if (keys.isEmpty() || entity == null) {
            return;
        }
        entity.getScheduler().scheduleDelayed(entity, () -> resync(keys), 1);
    }

    void resync(List<SlotKey> keys) {
        if (!player.isConnected()) {
            return;
        }
        for (var key : keys) {
            if (key.container().getContainerType() == ContainerTypes.CREATED_OUTPUT) {
                continue;
            }
            try {
                player.viewContainerSlot(key.container(), key.slot());
            } catch (IllegalStateException closed) {
                // Konteyner bu arada kapandı; açılışta zaten tamamı gönderilir.
            }
        }
    }

    private static ItemStack copyOf(ItemStack item) {
        if (item == null || item.getItemType() == ItemTypes.AIR) {
            return ItemAirStack.AIR_STACK;
        }
        // Aynı yığın kimliğiyle: geri alınca istemcinin tanıdığı kimlik korunur.
        return item.copy(false);
    }

    record SlotKey(Container container, int slot) {
    }
}
