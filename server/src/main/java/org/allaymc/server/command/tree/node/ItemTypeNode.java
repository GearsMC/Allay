package org.allaymc.server.command.tree.node;

import org.allaymc.api.command.tree.CommandContext;
import org.allaymc.api.command.tree.CommandNode;
import org.allaymc.api.item.type.ItemType;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.utils.identifier.Identifier;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author daoge_cmd
 */
public class ItemTypeNode extends BaseNode {

    public ItemTypeNode(String name, CommandNode parent, ItemType<?> defaultValue) {
        super(name, parent, defaultValue);
    }

    @Override
    public boolean match(CommandContext context) {
        var arg = context.queryArg();
        if (arg.indexOf(':') == -1) {
            arg = Identifier.DEFAULT_NAMESPACE + ":" + arg;
        }
        var itemType = Registries.ITEMS.get(new Identifier(arg));
        if (itemType == null) {
            context.addError("%" + TrKeys.MC_COMMANDS_GIVE_ITEM_NOTFOUND, arg);
            return false;
        }
        context.putResult(itemType);
        context.popArg();
        return true;
    }

    @Override
    public CommandParamData toNetworkData() {
        var data = super.toNetworkData();
        // NOTICE: The name must be "itemName", so that the client will show item list
        data.setName("itemName");
        // Soft "Item" enum: merge server-registered ids (including Education/chemistry)
        // with the client's built-in suggestions. An empty hard enum hid chemistry ids
        // because the client soft-list does not include Education content.
        Map<String, Set<CommandEnumConstraint>> values = new LinkedHashMap<>();
        for (var itemType : Registries.ITEMS.getContent().values()) {
            var identifier = itemType.getIdentifier();
            values.put(identifier.toString(), Collections.emptySet());
            values.put(identifier.path(), Collections.emptySet());
        }
        data.setEnumData(new CommandEnumData("Item", values, true));
        return data;
    }
}
