package org.allaymc.server.container.impl;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.container.ContainerViewer;
import org.allaymc.api.container.interfaces.FakeTradeContainer;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.player.Player;
import org.allaymc.api.trade.TradeOffer;
import org.allaymc.server.player.AllayPlayer;
import org.joml.Vector3d;

import java.util.List;
import java.util.function.Consumer;

/**
 * Köylü takas arayüzü (PMMP {@code TradeSession} + {@code TradeInventory} karşılığı).
 *
 * <p>Dünyaya sahte blok gönderilmez: arayüz {@code UpdateTradePacket} ile açılır ve
 * istemcide zaten görünen tüccar varlığına bağlanır. Açılış/kapanışta tüccarın
 * {@code TRADE_TARGET_EID} verisi yalnızca bu oyuncuya gönderilir. Takasın kendisi
 * {@code TradeActionHandler} içinde doğrulanır.</p>
 */
public class FakeTradeContainerImpl extends FakeContainerImpl implements FakeTradeContainer {
    /**
     * PMMP {@code TradeOfferList::NETWORK_ID_OFFSET}; istemciye giden {@code netId} bu
     * değerden başlar ve CraftRecipe isteğinde geri gelir.
     */
    public static final int NETWORK_ID_OFFSET = 500000;
    private static final int LEGACY_NETWORK_ID_OFFSET = 1000000;

    @Getter
    @Setter
    protected Entity trader;
    @Getter
    @Setter
    protected TradeListener tradeListener;
    protected volatile List<TradeOffer> offers = List.of();

    public FakeTradeContainerImpl() {
        super(ContainerTypes.FAKE_TRADE);
        // Oyuncu ödeme eşyalarını yuvalara kendisi taşır.
        this.interactable = true;
    }

    public static int networkIdFromIndex(int index) {
        return NETWORK_ID_OFFSET + index;
    }

    @Override
    public List<TradeOffer> getOffers() {
        return offers;
    }

    @Override
    public void setOffers(List<TradeOffer> offers) {
        if (offers == null || offers.isEmpty()) {
            throw new IllegalArgumentException("Takas listesi en az bir teklif içermeli");
        }
        this.offers = List.copyOf(offers);
    }

    /**
     * İstemcinin gönderdiği tarif kimliğini teklife çözer. PMMP ile aynı şekilde
     * hem düz hem zigzag kodlu değer, üç ofset için denenir.
     */
    public TradeOffer matchNetworkId(int networkId) {
        var list = this.offers;
        int zigzag = (networkId << 1) ^ (networkId >> 31);
        for (int candidate : zigzag == networkId ? new int[]{networkId} : new int[]{networkId, zigzag}) {
            for (int offset : new int[]{NETWORK_ID_OFFSET, LEGACY_NETWORK_ID_OFFSET, 0}) {
                int index = candidate - offset;
                if (index >= 0 && index < list.size()) {
                    return list.get(index);
                }
            }
        }
        return null;
    }

    @Override
    public void addPlayer(Player player, Consumer<Boolean> callback) {
        var entity = this.trader;
        if (entity == null || entity.isDespawned() || !(player instanceof AllayPlayer allayPlayer)) {
            callback.accept(false);
            return;
        }
        // Sahte blok gerekmediği için gecikme yok; tüccar zaten istemcide.
        sendTraderState(allayPlayer, playerUniqueId(allayPlayer));
        if (!addViewerDirectly(player)) {
            sendTraderState(allayPlayer, 0);
            callback.accept(false);
            return;
        }
        callback.accept(true);
    }

    @Override
    protected void onClose(ContainerViewer viewer) {
        if (viewer instanceof AllayPlayer player) {
            sendTraderState(player, 0);
            refundContents(player);
        }
        super.onClose(viewer);
    }

    @Override
    protected void sendFakeBlocks(Player player) {
        // Takas arayüzü bloğa bağlı değildir.
    }

    protected void refundContents(Player player) {
        var entity = player.getControlledEntity();
        for (int slot = 0; slot < getContainerType().getSize(); slot++) {
            ItemStack item = getItemStack(slot);
            if (item.isEmptyOrAir()) {
                continue;
            }
            clearSlot(slot, false);
            if (entity == null) {
                continue;
            }
            var inventory = entity.getContainer(ContainerTypes.INVENTORY);
            inventory.tryAddItem(item);
            if (item.getCount() > 0) {
                var location = entity.getLocation();
                entity.getDimension().dropItem(item, new Vector3d(location.x(), location.y(), location.z()));
            }
        }
    }

    protected void sendTraderState(AllayPlayer player, long playerUniqueId) {
        var entity = this.trader;
        if (entity == null || !player.isConnected()) {
            return;
        }
        var packet = player.getProtocol().getEncoder().encodeTraderState(entity, playerUniqueId);
        if (packet != null) {
            player.sendPacket(packet);
        }
    }

    public static long playerUniqueId(Player player) {
        return player.getControlledEntity().getUniqueId().getLeastSignificantBits();
    }
}
