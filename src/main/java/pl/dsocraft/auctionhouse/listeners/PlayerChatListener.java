package pl.dsocraft.auctionhouse.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

/**
 * Handles player chat events for the auction house.
 */
public class PlayerChatListener implements Listener {

    private final DSOAuctionHouse plugin;

    public PlayerChatListener(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles chat input for quantity selection when purchasing items.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if the player is awaiting chat input
        if (plugin.getGuiManager().isAwaitingChatInput(player.getUniqueId())) {
            event.setCancelled(true); // Cancel the chat event to prevent the message from being broadcast
            
            // Handle the chat input in the GUI manager
            plugin.getGuiManager().handleChatInput(player, event.getMessage());
        }
    }

    /**
     * Cleans up player data when they quit.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up any pending data for the player
        plugin.getGuiManager().cleanupPlayerData(player.getUniqueId());
    }
}