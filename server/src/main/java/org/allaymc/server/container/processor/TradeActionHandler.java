package org.allaymc.server.container.processor;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.container.Container;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.player.Player;
import org.allaymc.api.trade.TradeOffer;
import org.allaymc.server.container.impl.FakeTradeContainerImpl;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ConsumeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Takas penceresinden gelen CraftRecipe / AutoCraftRecipe isteklerini işler
 * (PMMP {@code ItemStackRequestExecutor::beginTradeFromRecipe} + {@code TradeTransaction}).
 *
 * <p>İstemci takasta şu sırayı gönderir: CraftRecipe(netId, adet), CraftResultsDeprecated,
 * ödeme yuvaları için Consume eylemleri, ardından ürünü CREATED_OUTPUT'tan alan Take/Place.
 * Allay eylemleri tek tek ve hemen uyguladığı için bütün doğrulama burada, hiçbir şey
 * değişmeden önce yapılır: sonraki Consume eylemlerinin toplamı teklifin fiyatına birebir
 * eşit olmalı ve yalnızca takas yuvalarından ya da oyuncu envanterinden gelmelidir.</p>
 */
@Slf4j
public final class TradeActionHandler {
    private static final int MAX_TRADES_PER_REQUEST = 256;
    private static final Set<ContainerSlotType> ALLOWED_SOURCES = Set.of(
            ContainerSlotType.TRADE2_INGREDIENT_1,
            ContainerSlotType.TRADE2_INGREDIENT_2,
            ContainerSlotType.TRADE_INGREDIENT_1,
            ContainerSlotType.TRADE_INGREDIENT_2,
            ContainerSlotType.HOTBAR,
            ContainerSlotType.INVENTORY,
            ContainerSlotType.HOTBAR_AND_INVENTORY
    );

    private TradeActionHandler() {
    }

    /**
     * @return oyuncunun açık takas penceresi, yoksa {@code null}
     */
    public static FakeTradeContainerImpl getOpenedTradeContainer(Player player) {
        return player.getOpenedContainer(ContainerTypes.FAKE_TRADE) instanceof FakeTradeContainerImpl trade ? trade : null;
    }

    public static ActionResponse handle(
            FakeTradeContainerImpl container,
            Player player,
            int recipeNetworkId,
            int tradeCount,
            int currentActionIndex,
            ItemStackRequestAction[] actions
    ) {
        if (tradeCount < 1 || tradeCount > MAX_TRADES_PER_REQUEST) {
            log.warn("Invalid trade count {} from {}", tradeCount, player.getOriginName());
            return ContainerActionProcessor.ERROR_RESPONSE;
        }

        var offer = container.matchNetworkId(recipeNetworkId);
        if (offer == null) {
            log.warn("Unknown trade recipe network id {} from {}", recipeNetworkId, player.getOriginName());
            return ContainerActionProcessor.ERROR_RESPONSE;
        }
        if (!offer.hasUsesLeft(tradeCount)) {
            return ContainerActionProcessor.ERROR_RESPONSE;
        }

        var result = offer.getSell();
        int resultCount = result.getCount() * tradeCount;
        if (resultCount > result.getItemType().getItemData().maxStackSize()) {
            log.warn("Trade result count {} exceeds max stack size", resultCount);
            return ContainerActionProcessor.ERROR_RESPONSE;
        }

        if (!consumesMatchPrice(player, offer, tradeCount, currentActionIndex, actions)) {
            log.warn("Trade payment mismatch for offer {} from {}", offer.getRecipeId(), player.getOriginName());
            return ContainerActionProcessor.ERROR_RESPONSE;
        }

        offer.addUses(tradeCount);
        ItemStackRequestTransaction.markIrreversible();
        var listener = container.getTradeListener();
        if (listener != null) {
            try {
                listener.onTrade(player, offer, tradeCount);
            } catch (Throwable throwable) {
                log.error("Trade listener failed", throwable);
            }
        }

        // Tek ürünlü tarif gibi: istemci CreateAction göndermez, ürün doğrudan CREATED_OUTPUT'a konur.
        var output = result.copy(true);
        output.setCount(resultCount);
        player.getControlledEntity().getContainer(ContainerTypes.CREATED_OUTPUT).setItemStack(0, output, false);
        return null;
    }

    private static boolean consumesMatchPrice(
            Player player,
            TradeOffer offer,
            int tradeCount,
            int currentActionIndex,
            ItemStackRequestAction[] actions
    ) {
        // Aynı eşya iki ödeme yuvasına bölünmüş olabilir (ör. 128 pul = 64 + 64); birleştir.
        List<ItemStack> required = new ArrayList<>(2);
        List<Integer> needed = new ArrayList<>(2);
        addRequirement(required, needed, offer.getBuyA(), tradeCount);
        var buyB = offer.getBuyB();
        if (buyB != null) {
            addRequirement(required, needed, buyB, tradeCount);
        }

        int[] consumed = new int[required.size()];
        // Aynı yuvadan birden çok Consume gelirse toplamı yığını aşmamalı.
        Map<Container, Map<Integer, Integer>> takenPerSlot = new IdentityHashMap<>();
        boolean any = false;
        for (int index = currentActionIndex + 1; index < actions.length; index++) {
            if (!(actions[index] instanceof ConsumeAction consume)) {
                continue;
            }
            any = true;
            var slotType = consume.source().containerName().container();
            if (!ALLOWED_SOURCES.contains(slotType)) {
                return false;
            }
            var source = ContainerActionProcessor.getContainerFrom(player, consume.source().containerName());
            if (source == null) {
                return false;
            }
            Integer slot = ContainerActionProcessor.fromNetworkSlotIndexOrNull(source, consume.source().slot());
            if (slot == null) {
                return false;
            }
            var item = source.getItemStack(slot);
            int taken = takenPerSlot.computeIfAbsent(source, $ -> new HashMap<>())
                    .merge(slot, consume.count(), Integer::sum);
            if (item.isEmptyOrAir() || consume.count() < 1 || taken > item.getCount()) {
                return false;
            }
            int bucket = -1;
            for (int i = 0; i < required.size(); i++) {
                if (required.get(i).canMerge(item, true)) {
                    bucket = i;
                    break;
                }
            }
            if (bucket < 0) {
                return false;
            }
            consumed[bucket] += consume.count();
        }

        if (!any) {
            return false;
        }
        for (int i = 0; i < required.size(); i++) {
            if (consumed[i] != needed.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static void addRequirement(List<ItemStack> required, List<Integer> needed, ItemStack stack, int tradeCount) {
        int amount = stack.getCount() * tradeCount;
        for (int i = 0; i < required.size(); i++) {
            if (required.get(i).canMerge(stack, true)) {
                needed.set(i, needed.get(i) + amount);
                return;
            }
        }
        required.add(stack);
        needed.add(amount);
    }
}
