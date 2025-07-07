package pl.dsocraft.auctionhouse.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

/**
 * Handles the /checkah command.
 */
public class CheckCommand implements CommandExecutor {

    private final DSOAuctionHouse plugin;

    public CheckCommand(DSOAuctionHouse plugin) {
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

        if (args.length < 1) {
            plugin.getMessageManager().sendRawMessage(player, "&cUsage: /checkah <player_name>");
            return true;
        }

        String targetName = args[0];
        
        // Try to find the player
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            plugin.getMessageManager().sendMessage(player, "player_not_found", "{player_name}", targetName);
            return true;
        }

        // Open the player items GUI
        plugin.getGuiManager().openPlayerItemsGUI(player, targetPlayer.getUniqueId(), targetPlayer.getName());
        return true;
    }
}