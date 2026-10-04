package org.allaymc.server.container.processor;

import org.allaymc.api.container.Container;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.player.Player;
import org.allaymc.api.server.Server;
import org.allaymc.server.container.impl.FakeContainerImpl;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RejectedFakeMenu {

    private static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<Set<Container>> MENUS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ABORT = new ThreadLocal<>();

    private RejectedFakeMenu() {
    }

    static void mark(Player player, Container... containers) {
        var menus = MENUS.get();
        if (menus == null) {
            menus = new LinkedHashSet<>();
            MENUS.set(menus);
        }
        if (containers != null) {
            for (var container : containers) {
                if (container instanceof FakeContainerImpl) {
                    menus.add(container);
                }
            }
        }
        PLAYER.set(player);
        ABORT.set(true);
    }

    static boolean consumeAbort() {
        var abort = Boolean.TRUE.equals(ABORT.get());
        ABORT.remove();
        return abort;
    }

    static ActionResponse unchanged(Player player, Container container, int slot) {
        mark(player, container);
        return new ActionResponse(true, List.of(snapshot(container, slot)));
    }

    static ActionResponse unchanged(Player player, Container source, int sourceSlot, Container destination, int destinationSlot) {
        mark(player, source, destination);
        var containers = new ArrayList<ItemStackResponseContainer>();
        if (source != null) {
            containers.add(snapshot(source, sourceSlot));
        }
        if (destination != null && (destination != source || destinationSlot != sourceSlot)) {
            containers.add(snapshot(destination, destinationSlot));
        }
        return new ActionResponse(true, containers);
    }

    static void resync() {
        var player = PLAYER.get();
        var menus = MENUS.get();
        clear();
        if (player == null) {
            return;
        }
        apply(player, menus == null ? Set.of() : menus);
    }

    static void scheduleResync() {
        var player = PLAYER.get();
        var menus = MENUS.get() == null ? Set.<Container>of() : Set.copyOf(MENUS.get());
        clear();
        if (player == null) {
            return;
        }

        var server = Server.getInstance();
        if (server == null) {
            apply(player, menus);
            return;
        }
        server.getScheduler().scheduleDelayed(server, () -> apply(player, menus), 1);
    }

    private static void clear() {
        PLAYER.remove();
        MENUS.remove();
        ABORT.remove();
    }

    private static void apply(Player player, Set<Container> menus) {
        var entity = player.getControlledEntity();
        if (entity != null) {
            refresh(player, entity.getContainer(ContainerTypes.INVENTORY));
            refresh(player, entity.getContainer(ContainerTypes.CURSOR));
            refresh(player, entity.getContainer(ContainerTypes.OFFHAND));
            refresh(player, entity.getContainer(ContainerTypes.ARMOR));
        }
        for (var menu : menus) {
            refresh(player, menu);
        }
    }

    private static void refresh(Player player, Container container) {
        if (container == null) {
            return;
        }
        var size = container.getContainerType().getSize();
        for (int slot = 0; slot < size; slot++) {
            try {
                player.viewContainerSlot(container, slot);
            } catch (IllegalStateException closed) {
                return;
            }
        }
    }

    private static ItemStackResponseContainer snapshot(Container container, int slot) {
        var item = container.getItemStack(slot);
        var slotType = ContainerActionProcessor.getSlotType(container, slot);
        var networkSlot = ContainerActionProcessor.toNetworkSlotIndex(container, slot);
        return new ItemStackResponseContainer(
                slotType,
                List.of(new ItemStackResponseSlot(
                        networkSlot,
                        networkSlot,
                        item.getCount(),
                        item.getUniqueId(),
                        item.getCustomName(),
                        item.getDamage(),
                        ""
                )),
                new FullContainerName(slotType, null)
        );
    }
}
