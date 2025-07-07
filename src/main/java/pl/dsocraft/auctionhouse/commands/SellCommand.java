package pl.dsocraft.auctionhouse.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;
import pl.dsocraft.auctionhouse.utils.PriceParser;

/**
 * Handles the /sell command.
 */
public class SellCommand implements CommandExecutor {

    private final DSOAuctionHouse plugin;

    public SellCommand(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "player_only_command");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ah.sell")) {
            plugin.getMessageManager().sendMessage(player, "no_permission");
            return true;
        }

        if (args.length < 1) {
            plugin.getMessageManager().sendRawMessage(player, "&cUsage: /sell <price>");
            return true;
        }

        // Get the item in the player's hand
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            plugin.getMessageManager().sendMessage(player, "item_not_in_hand");
            return true;
        }

        // Parse the price
        long price = PriceParser.parsePrice(args[0]);
        if (price <= 0) {
            plugin.getMessageManager().sendMessage(player, "invalid_price_format");
            return true;
        }

        // List the item for auction
        boolean success = plugin.getAuctionManager().listItem(player, itemInHand, price);

        // The success message is sent by the AuctionManager
        return true;
    }
}