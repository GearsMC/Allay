package org.allaymc.server.network.processor.ingame;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.player.Player;
import org.allaymc.server.container.processor.ActionResponse;
import org.allaymc.server.container.processor.ContainerActionProcessor;
import org.allaymc.server.container.processor.ContainerActionProcessorHolder;
import org.allaymc.server.container.processor.ItemStackRequestTransaction;
import org.allaymc.server.network.processor.PacketProcessor;
import org.allaymc.server.player.AllayPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketType;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;

import java.util.*;

import static org.allaymc.server.container.processor.CraftRecipeOptionalActionProcessor.FILTER_STRINGS_DATA_KEY;

/**
 * @author Cool_Loong
 */
@Slf4j
public class ItemStackRequestPacketProcessor extends PacketProcessor<ItemStackRequestPacket> {
    protected final ContainerActionProcessorHolder processorHolder = new ContainerActionProcessorHolder();

    @Override
    public void handleSync(Player player, ItemStackRequestPacket packet, long receiveTime) {
        try {
            handleRequests(player, packet);
        } finally {
            ContainerActionProcessor.scheduleRejectedFakeMenuResync();
        }
    }

    private void handleRequests(Player player, ItemStackRequestPacket packet) {
        List<ItemStackResponse> encodedResponses = new LinkedList<>();
        for (var request : packet.getRequests()) {
            var transaction = ItemStackRequestTransaction.begin(player);
            try {
                encodedResponses.add(handleRequest(player, request, transaction));
            } finally {
                transaction.close();
            }
        }

        var allayPlayer = (AllayPlayer) player;
        allayPlayer.sendPacket(allayPlayer.getProtocol().getEncoder().encodeItemStackResponse(encodedResponses));
    }

    private ItemStackResponse handleRequest(Player player, ItemStackRequest request, ItemStackRequestTransaction transaction) {
        // It is possible to have two same type actions in one request!
        List<ActionResponse> responses = new LinkedList<>();
        // Indicate that the further destroy action does not return a response
        // For more details, see inventory_stack_packet.md
        var noResponseForDestroyAction = false;
        var actions = request.actions();

        Map<String, Object> dataPool = new HashMap<>();
        dataPool.put(FILTER_STRINGS_DATA_KEY, request.filterStrings());

        for (int index = 0; index < actions.length; index++) {
            var action = actions[index];
            if (action.getType() == ItemStackRequestActionType.CRAFT_RESULTS_DEPRECATED) {
                noResponseForDestroyAction = true;
            }

            ContainerActionProcessor<ItemStackRequestAction> processor = processorHolder.getProcessor(action.getType());
            if (processor == null) {
                log.warn("Not found handler for action type {}", action.getType());
                continue;
            }

            // GearsMC fork: eylem yuvaları değiştirmeden önce ilk halleri saklanır (bkz. ItemStackRequestTransaction).
            transaction.capture(action);
            ActionResponse response;
            try {
                response = processor.handle(action, player, index, actions, dataPool);
            } catch (RuntimeException exception) {
                log.error("Item stack request action {} of {} failed", action.getType(), player.getOriginName(), exception);
                response = ContainerActionProcessor.ERROR_RESPONSE;
            }
            if (response == null) {
                continue;
            }

            if (ContainerActionProcessor.consumeRejectedFakeMenuAbort()) {
                if (response.ok()) {
                    transaction.commit();
                    return encodeActionResponses(List.of(response), request.requestId());
                }
                return reject(player, request, action, index, transaction);
            }

            if (!response.ok()) {
                return reject(player, request, action, index, transaction);
            }

            if (noResponseForDestroyAction && action.getType() == ItemStackRequestActionType.DESTROY) {
                noResponseForDestroyAction = false;
            } else {
                responses.add(response);
            }
        }

        transaction.commit();
        return encodeActionResponses(responses, request.requestId());
    }

    /**
     * İstemci reddedilen isteğin tamamını geri alır; sunucu da (yan etki yoksa) geri alır ve istemciyi
     * gerçek içerikle eşitler.
     */
    private ItemStackResponse reject(Player player, ItemStackRequest request, ItemStackRequestAction action,
                                     int index, ItemStackRequestTransaction transaction) {
        var rolledBack = transaction.rollback();
        // Geri almadan sonra: istemcinin isteği gönderdiği andaki sunucu içeriği.
        var slots = transaction.describeSlots();
        log.warn("Item stack request rejected: player={}, action={} (#{} of {}), slots={}, rolledBack={}",
                player.getOriginName(), action.getType(), index + 1, request.actions().length, slots, rolledBack);
        return new ItemStackResponse(ItemStackResponseStatus.ERROR, request.requestId(), null);
    }

    private ItemStackResponse encodeActionResponses(List<ActionResponse> responses, int requestId) {
        Map<ContainerSlotType, Int2ObjectMap<ItemStackResponseSlot>> changedContainers = new HashMap<>();
        responses.forEach(response -> response.containers().forEach(container -> {
            for (var changedSlot : container.items()) {
                var changedSlots = changedContainers.computeIfAbsent(container.containerName().container(), $ -> new Int2ObjectOpenHashMap<>());
                changedSlots.put(changedSlot.getSlot(), changedSlot);
            }
        }));

        var containers = changedContainers.entrySet().stream()
                .map(entry -> new ItemStackResponseContainer(entry.getKey(), new ArrayList<>(entry.getValue().values()), new FullContainerName(entry.getKey(), null)))
                .toList();
        return new ItemStackResponse(ItemStackResponseStatus.OK, requestId, containers);
    }

    @Override
    public BedrockPacketType getPacketType() {
        return BedrockPacketType.ITEM_STACK_REQUEST;
    }
}
