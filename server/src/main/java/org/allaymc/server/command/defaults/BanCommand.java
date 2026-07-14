package org.allaymc.server.command.defaults;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.permission.Permissions;
import org.allaymc.api.server.Server;

/**
 * @author daoge_cmd
 */
public class BanCommand extends Command {

    public BanCommand() {
        super("ban", TrKeys.MC_COMMANDS_BAN_DESCRIPTION, Permissions.COMMAND_BAN);
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot().str("name").exec(context -> {
            String name = context.getResult(0);

            var manager = Server.getInstance().getPlayerManager();
            var xuid = manager.resolveXuid(name).orElse(null);
            if (xuid == null) {
                context.addError("%" + TrKeys.MC_COMMANDS_GENERIC_PLAYER_NOTFOUND);
                return context.fail();
            }

            if (manager.ban(xuid)) {
                context.addOutput(TrKeys.MC_COMMANDS_BAN_SUCCESS, name);
                return context.success();
            } else {
                context.addError("%" + TrKeys.MC_COMMANDS_BAN_FAILED, name);
                return context.fail();
            }
        });
    }
}
