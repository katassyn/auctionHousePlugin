package pl.dsocraft.auctionhouse.managers;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

/**
 * Manages sending messages to players with proper formatting.
 */
public class MessageManager {

    private final DSOAuctionHouse plugin;
    private final String prefix;

    public MessageManager(DSOAuctionHouse plugin) {
        this.plugin = plugin;
        this.prefix = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "&7[&6AH&7] "));
    }

    /**
     * Sends a message to a command sender (player or console).
     *
     * @param sender The command sender to send the message to.
     * @param messageKey The key of the message in the config.
     */
    public void sendMessage(CommandSender sender, String messageKey) {
        String message = getMessageFromConfig(messageKey);
        if (!message.isEmpty()) {
            sender.sendMessage(prefix + message);
        }
    }

    /**
     * Sends a message to a command sender with replacements.
     *
     * @param sender The command sender to send the message to.
     * @param messageKey The key of the message in the config.
     * @param replacements An array of key-value pairs for replacements (e.g., "{player}", "Steve").
     */
    public void sendMessage(CommandSender sender, String messageKey, String... replacements) {
        String message = getMessageFromConfig(messageKey);
        if (!message.isEmpty()) {
            message = applyReplacements(message, replacements);
            sender.sendMessage(prefix + message);
        }
    }

    /**
     * Sends a raw message without using the config.
     *
     * @param sender The command sender to send the message to.
     * @param message The message to send.
     */
    public void sendRawMessage(CommandSender sender, String message) {
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    /**
     * Gets a message from the config and translates color codes.
     *
     * @param messageKey The key of the message in the config.
     * @return The formatted message.
     */
    public String getMessageFromConfig(String messageKey) {
        String message = plugin.getConfig().getString("messages." + messageKey, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Applies replacements to a message.
     *
     * @param message The message to apply replacements to.
     * @param replacements An array of key-value pairs for replacements.
     * @return The message with replacements applied.
     */
    private String applyReplacements(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Invalid number of replacement parameters for message: " + message);
            return message;
        }

        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    /**
     * Formats a price for display.
     *
     * @param price The price to format.
     * @return The formatted price.
     */
    public String formatPrice(long price) {
        if (price >= 1_000_000_000) {
            return String.format("%.2fb", price / 1_000_000_000.0);
        } else if (price >= 1_000_000) {
            return String.format("%.2fm", price / 1_000_000.0);
        } else if (price >= 1_000) {
            return String.format("%.2fk", price / 1_000.0);
        } else {
            return String.valueOf(price);
        }
    }

    /**
     * Gets the prefix used for messages.
     *
     * @return The prefix.
     */
    public String getPrefix() {
        return prefix;
    }
}