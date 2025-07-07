package pl.dsocraft.auctionhouse.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

/**
 * Handles inventory click events for the auction house GUIs.
 */
public class InventoryClickListener implements Listener {

    private final DSOAuctionHouse plugin;

    public InventoryClickListener(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();

        if (inventory == null) {
            return;
        }

        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());

        // Check if this is one of our GUIs
        if (inventoryTitle.contains("Auction House") || 
            inventoryTitle.contains("'s Auctions") || 
            inventoryTitle.equals("Confirm Purchase") ||
            inventoryTitle.equals("Your Mailbox")) {

            event.setCancelled(true); // Cancel the event to prevent item movement

            int slot = event.getSlot();
            boolean isRightClick = event.getClick() == ClickType.RIGHT;
            boolean isShiftClick = event.isShiftClick();
            boolean isMiddleClick = event.getClick() == ClickType.MIDDLE;

            // Handle different GUIs
            if (inventoryTitle.equals("Confirm Purchase")) {
                plugin.getGuiManager().handlePurchaseConfirmClick(player, slot, isRightClick, isMiddleClick);
            } else if (inventoryTitle.contains("Auction House")) {
                // First check if it's a navigation button
                if (plugin.getGuiManager().handleNavigationClick(player, slot, inventoryTitle)) {
                    return;
                }

                // Then check if it's a player head (in main GUI)
                plugin.getGuiManager().handlePlayerHeadClick(player, slot);
            } else if (inventoryTitle.contains("'s Auctions")) {
                // First check if it's a navigation button
                if (plugin.getGuiManager().handleNavigationClick(player, slot, inventoryTitle)) {
                    return;
                }

                // Then check if it's an auction item
                plugin.getGuiManager().handleAuctionItemClick(player, slot, isRightClick, isShiftClick);
            } else if (inventoryTitle.equals("Your Mailbox")) {
                // First check if it's a navigation button
                if (plugin.getGuiManager().handleNavigationClick(player, slot, inventoryTitle)) {
                    return;
                }

                // Then check if it's a mailbox item
                plugin.getGuiManager().handleMailboxItemClick(player, slot);
            }
        }
    }
}
