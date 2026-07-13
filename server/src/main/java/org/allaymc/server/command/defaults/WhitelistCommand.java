package org.allaymc.server.command.defaults;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.permission.Permissions;
import org.allaymc.api.server.Server;

/**
 * @author daoge_cmd
 */
public class WhitelistCommand extends Command {
    public WhitelistCommand() {
        super("whitelist", TrKeys.MC_COMMANDS_ALLOWLIST_DESCRIPTION, Permissions.COMMAND_WHITELIST);
        aliases.add("allowlist");
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .enums("operation", "add", "remove")
                .str("name")
                .exec(context -> {
                    String operation = context.getResult(0);
                    String name = context.getResult(1);

                    var manager = Server.getInstance().getPlayerManager();
                    var xuid = manager.resolveXuid(name).orElse(null);
                    if (xuid == null) {
                        context.addError("%" + TrKeys.MC_COMMANDS_GENERIC_PLAYER_NOTFOUND);
                        return context.fail();
                    }

                    switch (operation) {
                        case "add" -> {
                            if (manager.addToWhitelist(xuid)) {
                                context.addOutput(TrKeys.MC_COMMANDS_ALLOWLIST_ADD_SUCCESS, name);
                                return context.success();
                            } else {
                                context.addError("%" + TrKeys.MC_COMMANDS_ALLOWLIST_ADD_FAILED, name);
                                return context.fail();
                            }
                        }
                        case "remove" -> {
                            if (manager.removeFromWhitelist(xuid)) {
                                context.addOutput(TrKeys.MC_COMMANDS_ALLOWLIST_REMOVE_SUCCESS, name);
                                return context.success();
                            } else {
                                context.addError("%" + TrKeys.MC_COMMANDS_ALLOWLIST_REMOVE_FAILED, name);
                                return context.fail();
                            }
                        }
                        default -> {
                            // Won't happen
                            return context.fail();
                        }
                    }
                })
                .root()
                .key("list")
                .exec(context -> {
                    var whitelist = Server.getInstance().getPlayerManager().getWhitelistedPlayers();
                    var onlineCount = (int) Server.getInstance().getPlayerManager().getPlayers().values().stream()
                            .filter(player -> whitelist.contains(player.getXuid()))
                            .count();
                    context.addOutput(TrKeys.MC_COMMANDS_ALLOWLIST_LIST, whitelist.size(), onlineCount);
                    context.addOutput(String.join(", ", whitelist));
                    return context.success();
                })
                .root()
                .key("enable")
                .exec(context -> {
                    Server.getInstance().getPlayerManager().setWhitelistStatus(true);
                    context.addOutput(TrKeys.MC_COMMANDS_ALLOWLIST_ENABLED);
                    return context.success();
                })
                .root()
                .key("disable")
                .exec(context -> {
                    Server.getInstance().getPlayerManager().setWhitelistStatus(false);
                    context.addOutput(TrKeys.MC_COMMANDS_ALLOWLIST_DISABLED);
                    return context.success();
                });
    }
}
