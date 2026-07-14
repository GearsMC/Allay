package org.allaymc.server.command.defaults;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.permission.Permissions;
import org.allaymc.api.server.Server;

/**
 * @author xingchentye
 */
public class DeOpCommand extends Command {

    public DeOpCommand() {
        super("deop", TrKeys.MC_COMMANDS_DEOP_DESCRIPTION, Permissions.COMMAND_DEOP);
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot().wildcardTarget("player").exec(context -> {
            String input = context.getResult(0);

            var manager = Server.getInstance().getPlayerManager();
            var xuid = manager.resolveXuid(input).orElse(null);
            if (xuid == null) {
                context.addError("%" + TrKeys.MC_COMMANDS_GENERIC_PLAYER_NOTFOUND);
                return context.fail();
            }

            if (!manager.isOperator(xuid)) {
                context.addError("%" + TrKeys.MC_COMMANDS_DEOP_FAILED, input);
                return context.fail();
            }

            manager.setOperator(xuid, false);
            context.addOutput(TrKeys.MC_COMMANDS_DEOP_SUCCESS, input);
            var onlinePlayer = manager.getPlayerByXuid(xuid);
            if (onlinePlayer != null) {
                onlinePlayer.sendTranslatable(TrKeys.MC_COMMANDS_DEOP_MESSAGE);
            }
            return context.success();
        });
    }
}