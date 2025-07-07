package pl.dsocraft.auctionhouse.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;
import pl.dsocraft.auctionhouse.database.AuctionItem;

import java.util.List;
import java.util.UUID;

/**
 * Handles the /ah command.
 */
public class AuctionCommand implements CommandExecutor {

    private final DSOAuctionHouse plugin;

    public AuctionCommand(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "player_only_command");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ah.use")) {
            plugin.getMessageManager().sendMessage(player, "no_permission");
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "find":
                    return handleFindCommand(player, args);
                case "mailbox":
                    return handleMailboxCommand(player);
                case "help":
                    return handleHelpCommand(player);
                default:
                    // If the subcommand is not recognized, open the main GUI
                    plugin.getGuiManager().openMainGUI(player);
                    return true;
            }
        }

        // No arguments, open the main GUI
        plugin.getGuiManager().openMainGUI(player);
        return true;
    }

    /**
     * Handles the /ah find command.
     *
     * @param player The player who executed the command.
     * @param args The command arguments.
     * @return true if the command was handled, false otherwise.
     */
    private boolean handleFindCommand(Player player, String[] args) {
        if (!player.hasPermission("ah.find")) {
            plugin.getMessageManager().sendMessage(player, "no_permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendRawMessage(player, "&cUsage: /ah find <search_term>");
            return true;
        }

        // Join all arguments after "find" to create the search term
        StringBuilder searchTerm = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                searchTerm.append(" ");
            }
            searchTerm.append(args[i]);
        }

        // Open the main GUI with search filtering
        plugin.getGuiManager().openMainGUI(player, searchTerm.toString());
        return true;
    }

    /**
     * Handles the /ah mailbox command.
     *
     * @param player The player who executed the command.
     * @return true if the command was handled, false otherwise.
     */
    private boolean handleMailboxCommand(Player player) {
        // Open the mailbox GUI
        plugin.getGuiManager().openMailboxGUI(player);
        return true;
    }

    /**
     * Handles the /ah help command.
     *
     * @param player The player who executed the command.
     * @return true if the command was handled, false otherwise.
     */
    private boolean handleHelpCommand(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "&6=== &eAuction House Help &6===");
        plugin.getMessageManager().sendRawMessage(player, "&e/ah &7- Open the Auction House");
        plugin.getMessageManager().sendRawMessage(player, "&e/ah find <search_term> &7- Search for items");
        plugin.getMessageManager().sendRawMessage(player, "&e/ah mailbox &7- Open your mailbox");
        plugin.getMessageManager().sendRawMessage(player, "&e/sell <price> &7- Sell the item in your hand");
        plugin.getMessageManager().sendRawMessage(player, "&e/checkah <player> &7- Check a player's auctions");

        if (player.hasPermission("ah.admin")) {
            plugin.getMessageManager().sendRawMessage(player, "&e/ahadmin &7- Admin commands");
        }

        return true;
    }
}
