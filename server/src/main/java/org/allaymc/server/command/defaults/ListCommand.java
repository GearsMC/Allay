package org.allaymc.server.command.defaults;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.permission.Permissions;
import org.allaymc.api.server.Server;

import java.util.StringJoiner;

/**
 * @author daoge_cmd
 */
public class ListCommand extends Command {
    public ListCommand() {
        // GearsMC forkunda ad "allaylist": /list adi sunucunun kendi oyuncu listesine
        // (yetkili/oyuncu ayrimi, gorunmezlik suzgeci) aittir. Ayni adla kayit yapilsaydi
        // biri digerinin uzerine sessizce yazardi.
        super("allaylist", TrKeys.MC_COMMANDS_LIST_DESCRIPTION, Permissions.COMMAND_LIST);
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot().exec(context -> {
            var playerManager = Server.getInstance().getPlayerManager();
            var players = playerManager.getPlayers().values();

            var joiner = new StringJoiner(", ");
            players.forEach(player -> joiner.add(player.getControlledEntity().getDisplayName()));

            context.getSender().sendTranslatable(TrKeys.MC_COMMANDS_PLAYERS_LIST, players.size(), playerManager.getMaxPlayerCount());
            context.getSender().sendMessage(joiner.toString());
            return context.success();
        });
    }
}
