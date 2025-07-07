package pl.dsocraft.auctionhouse.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

/**
 * Handles the /ahadmin command.
 */
public class AdminCommand implements CommandExecutor {

    private final DSOAuctionHouse plugin;

    public AdminCommand(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ah.admin")) {
            plugin.getMessageManager().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 1) {
            sendAdminHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
            case "help":
                sendAdminHelp(sender);
                return true;
            default:
                plugin.getMessageManager().sendRawMessage(sender, "&cUnknown admin command. Use /ahadmin help for a list of commands.");
                return true;
        }
    }

    /**
     * Handles the /ahadmin reload command.
     *
     * @param sender The command sender.
     * @return true if the command was handled, false otherwise.
     */
    private boolean handleReloadCommand(CommandSender sender) {
        // Reload the plugin's configuration
        plugin.reloadConfig();
        plugin.getMessageManager().sendRawMessage(sender, "&aAuction House configuration reloaded.");
        return true;
    }

    /**
     * Sends the admin help message.
     *
     * @param sender The command sender.
     */
    private void sendAdminHelp(CommandSender sender) {
        plugin.getMessageManager().sendRawMessage(sender, "&6=== &eAuction House Admin Help &6===");
        plugin.getMessageManager().sendRawMessage(sender, "&e/ahadmin reload &7- Reload the plugin configuration");
        plugin.getMessageManager().sendRawMessage(sender, "&e/ahadmin help &7- Show this help message");
    }
}