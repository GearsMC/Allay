package org.allaymc.server.player;

import org.allaymc.server.network.AllayNetworkInterface;
import org.allaymc.testutils.AllayTestExtension;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AllayTestExtension.class)
class ClientBlockRulesTest {

    private static AllayPlayer newPlayer() {
        return new AllayPlayer(mock(BedrockServerSession.class), mock(AllayNetworkInterface.class)) {
            @Override
            protected void onDisconnect(String disconnectReason) {
            }
        };
    }

    private static ItemData item(String id) {
        var definition = mock(ItemDefinition.class);
        when(definition.identifier()).thenReturn(id);
        return ItemData.builder().definition(definition).count(1).build();
    }

    @Test
    void emptyRulesLeaveItemsUntouched() {
        var player = newPlayer();
        var data = item("minecraft:stone");
        assertFalse(player.hasClientBlockRules());
        assertSame(data, player.withClientBlockRules(data));
    }

    @Test
    void rulesAddCanDestroyToEveryItemAndCanPlaceOnToMatchingItem() {
        var player = newPlayer();
        player.setClientBlockRules(List.of("minecraft:coal_ore"),
                Map.of("minecraft:chemical_heat", List.of("minecraft:reinforced_deepslate")));
        assertTrue(player.hasClientBlockRules());

        var stone = player.withClientBlockRules(item("minecraft:stone"));
        assertArrayEquals(new String[]{"minecraft:coal_ore"}, stone.getCanBreak());
        assertEquals(0, stone.getCanPlace().length);

        var heat = player.withClientBlockRules(item("minecraft:chemical_heat"));
        assertArrayEquals(new String[]{"minecraft:reinforced_deepslate"}, heat.getCanPlace());
    }

    @Test
    void clearingRulesRestores() {
        var player = newPlayer();
        player.setClientBlockRules(List.of("minecraft:coal_ore"), Map.of());
        player.setClientBlockRules(List.of(), Map.of());
        assertFalse(player.hasClientBlockRules());
    }
}
