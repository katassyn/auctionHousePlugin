package pl.dsocraft.auctionhouse.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;
import pl.dsocraft.auctionhouse.database.AuctionItem;
import pl.dsocraft.auctionhouse.database.DatabaseManager;
import pl.dsocraft.auctionhouse.database.MailboxItem;
import pl.dsocraft.auctionhouse.utils.Paginator;

import java.util.*;

/**
 * Manages GUI creation and interaction.
 */
public class GUIManager {

    private final DSOAuctionHouse plugin;

    // Different paginators for different GUI types
    private final Map<UUID, Paginator<DatabaseManager.PlayerAuctionInfo>> playerHeadPaginators = new HashMap<>();
    private final Map<UUID, Paginator<AuctionItem>> playerItemPaginators = new HashMap<>();
    private final Map<UUID, Paginator<MailboxItem>> mailboxPaginators = new HashMap<>();

    // Purchase-related data
    private final Map<UUID, Integer> pendingPurchases = new HashMap<>();
    private final Map<UUID, Integer> pendingPurchaseQuantities = new HashMap<>();
    private final Set<UUID> awaitingChatInput = new HashSet<>();

    // GUI constants
    private static final int INVENTORY_SIZE = 54; // 6 rows
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of items
    private static final int MAILBOX_BUTTON_SLOT = 46;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int MAILBOX_SLOT = 46; // Bottom-left slot in main GUI
    private static final int BACK_BUTTON_SLOT = 49; // Back navigation in sub GUIs

    public GUIManager(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main auction house GUI showing player heads.
     */
    public void openMainGUI(Player player) {
        openMainGUI(player, null);
    }

    /**
     * Opens the main auction house GUI with optional search filtering.
     */
    public void openMainGUI(Player player, String searchTerm) {
        List<DatabaseManager.PlayerAuctionInfo> players;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            players = plugin.getAuctionManager().getPlayersWithMatchingItems(searchTerm);
        } else {
            players = plugin.getAuctionManager().getPlayersWithAuctions();
        }

        Paginator<DatabaseManager.PlayerAuctionInfo> paginator = new Paginator<>(players, ITEMS_PER_PAGE);
        playerHeadPaginators.put(player.getUniqueId(), paginator);

        updateMainGUI(player, searchTerm);
    }

    /**
     * Updates the main auction house GUI.
     */
    public void updateMainGUI(Player player, String searchTerm) {
        Paginator<DatabaseManager.PlayerAuctionInfo> paginator = playerHeadPaginators.get(player.getUniqueId());
        if (paginator == null) {
            openMainGUI(player, searchTerm);
            return;
        }

        String titleTemplate = plugin.getConfig().getString("gui.main_title", "&1&lDSO Auction House");
        String title = ChatColor.translateAlternateColorCodes('&', titleTemplate);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            title += " - Search: " + searchTerm;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Add player heads for the current page
        List<DatabaseManager.PlayerAuctionInfo> pageItems = paginator.getCurrentPageItems();
        int slot = 0;
        for (DatabaseManager.PlayerAuctionInfo playerInfo : pageItems) {
            inventory.setItem(slot, createPlayerHeadItemStack(playerInfo));
            slot++;
        }

        // Fill empty slots with filler item
        ItemStack fillerItem = createFillerItem();
        for (int i = pageItems.size(); i < ITEMS_PER_PAGE; i++) {
            inventory.setItem(i, fillerItem);
        }

        // Add navigation buttons
        if (paginator.hasPreviousPage()) {
            inventory.setItem(PREV_PAGE_SLOT, createNavigationButton(
                    Material.ARROW, plugin.getConfig().getString("gui.buttons.previous_page", "&cPrevious Page")));
        } else {
            inventory.setItem(PREV_PAGE_SLOT, fillerItem);
        }

        if (paginator.hasNextPage()) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationButton(
                    Material.ARROW, plugin.getConfig().getString("gui.buttons.next_page", "&aNext Page")));
        } else {
            inventory.setItem(NEXT_PAGE_SLOT, fillerItem);
        }

        // Add mailbox shortcut
        inventory.setItem(MAILBOX_SLOT, createNavigationButton(

                Material.CHEST, plugin.getConfig().getString("gui.buttons.mailbox", "&eMailbox")));

        // Fill remaining slots with filler item
        for (int i = ITEMS_PER_PAGE; i < INVENTORY_SIZE; i++) {
            if (i != PREV_PAGE_SLOT && i != NEXT_PAGE_SLOT && i != MAILBOX_SLOT) {

                inventory.setItem(i, fillerItem);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Opens the player items GUI for a player.
     */
    public void openPlayerItemsGUI(Player player, UUID targetUUID, String targetName) {
        List<AuctionItem> items = plugin.getAuctionManager().getPlayerAuctionItems(targetUUID);
        Paginator<AuctionItem> paginator = new Paginator<>(items, ITEMS_PER_PAGE);
        playerItemPaginators.put(player.getUniqueId(), paginator);

        updatePlayerItemsGUI(player, targetName);
    }

    /**
     * Updates the player items GUI for a player.
     */
    public void updatePlayerItemsGUI(Player player, String targetName) {
        Paginator<AuctionItem> paginator = playerItemPaginators.get(player.getUniqueId());
        if (paginator == null) {
            return;
        }

        String titleTemplate = plugin.getConfig().getString("gui.player_items_title_template", "&1&l{player_name}'s Auctions");
        String title = ChatColor.translateAlternateColorCodes('&', titleTemplate.replace("{player_name}", targetName));

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Add items for the current page
        List<AuctionItem> pageItems = paginator.getCurrentPageItems();
        int slot = 0;
        for (AuctionItem item : pageItems) {
            inventory.setItem(slot, createAuctionItemStack(item, player));
            slot++;
        }

        // Fill empty slots with filler item
        ItemStack fillerItem = createFillerItem();
        for (int i = pageItems.size(); i < ITEMS_PER_PAGE; i++) {
            inventory.setItem(i, fillerItem);
        }

        // Add navigation buttons
        if (paginator.hasPreviousPage()) {
            inventory.setItem(PREV_PAGE_SLOT, createNavigationButton(
                    Material.ARROW, 
                    plugin.getConfig().getString("gui.buttons.previous_page", "&cPrevious Page")));
        } else {
            inventory.setItem(PREV_PAGE_SLOT, fillerItem);
        }

        if (paginator.hasNextPage()) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationButton(
                    Material.ARROW, 
                    plugin.getConfig().getString("gui.buttons.next_page", "&aNext Page")));
        } else {
            inventory.setItem(NEXT_PAGE_SLOT, fillerItem);
        }

        // Add back button
        inventory.setItem(BACK_BUTTON_SLOT, createNavigationButton(
                Material.BARRIER,
                plugin.getConfig().getString("gui.buttons.back", "&eBack")));

        // Fill remaining slots with filler item
        for (int i = ITEMS_PER_PAGE; i < INVENTORY_SIZE; i++) {
            if (i != PREV_PAGE_SLOT && i != NEXT_PAGE_SLOT && i != BACK_BUTTON_SLOT) {
                inventory.setItem(i, fillerItem);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Opens the mailbox GUI for a player.
     */
    public void openMailboxGUI(Player player) {
        List<MailboxItem> items = plugin.getAuctionManager().getPlayerMailboxItems(player.getUniqueId());
        Paginator<MailboxItem> paginator = new Paginator<>(items, ITEMS_PER_PAGE);
        mailboxPaginators.put(player.getUniqueId(), paginator);

        updateMailboxGUI(player);
    }

    /**
     * Updates the mailbox GUI for a player.
     */
    public void updateMailboxGUI(Player player) {
        Paginator<MailboxItem> paginator = mailboxPaginators.get(player.getUniqueId());
        if (paginator == null) {
            openMailboxGUI(player);
            return;
        }

        String titleTemplate = plugin.getConfig().getString("gui.mailbox_title", "&1&lYour Mailbox");
        String title = ChatColor.translateAlternateColorCodes('&', titleTemplate);

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Add mailbox items for the current page
        List<MailboxItem> pageItems = paginator.getCurrentPageItems();
        int slot = 0;
        for (MailboxItem item : pageItems) {
            inventory.setItem(slot, createMailboxItemStack(item));
            slot++;
        }

        // Fill empty slots with filler item
        ItemStack fillerItem = createFillerItem();
        for (int i = pageItems.size(); i < ITEMS_PER_PAGE; i++) {
            inventory.setItem(i, fillerItem);
        }

        // Add navigation buttons
        if (paginator.hasPreviousPage()) {
            inventory.setItem(PREV_PAGE_SLOT, createNavigationButton(
                    Material.ARROW, "Previous Page"));
        } else {
            inventory.setItem(PREV_PAGE_SLOT, fillerItem);
        }

        if (paginator.hasNextPage()) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationButton(
                    Material.ARROW, "Next Page"));
        } else {
            inventory.setItem(NEXT_PAGE_SLOT, fillerItem);
        }

        // Add back button
        inventory.setItem(BACK_BUTTON_SLOT, createNavigationButton(
                Material.BARRIER, "Back"));

        // Fill remaining slots with filler item
        for (int i = ITEMS_PER_PAGE; i < INVENTORY_SIZE; i++) {
            if (i != PREV_PAGE_SLOT && i != NEXT_PAGE_SLOT && i != BACK_BUTTON_SLOT) {
                inventory.setItem(i, fillerItem);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Opens the purchase confirmation GUI for a player.
     *
     * @param player The player to open the GUI for.
     * @param auctionItem The auction item being purchased.
     */
    public void openPurchaseConfirmGUI(Player player, AuctionItem auctionItem) {
        pendingPurchases.put(player.getUniqueId(), auctionItem.getId());

        String title = ChatColor.translateAlternateColorCodes('&', "&aConfirm Purchase");
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        // Item being purchased in the center
        inventory.setItem(13, createAuctionItemStack(auctionItem, player));

        // Confirmation button setup depends on stackability
        boolean isStackable = auctionItem.getItemStack().getMaxStackSize() > 1;

        ItemStack confirmButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        List<String> confirmLore = new ArrayList<>();

        if (isStackable) {
            String buyAllText = plugin.getConfig().getString("gui.buttons.confirm_purchase_lmb", "&aBuy All (LMB)");
            String buyOneText = plugin.getConfig().getString("gui.buttons.confirm_purchase_rmb", "&aBuy One (RMB)");
            String buyAmountText = plugin.getConfig().getString("gui.buttons.confirm_purchase_mmb", "&aBuy Amount (MMB)");

            confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', buyAllText));
            confirmLore.add(ChatColor.translateAlternateColorCodes('&', buyOneText));
            confirmLore.add(ChatColor.translateAlternateColorCodes('&', buyAmountText));
            confirmLore.add("");
            confirmLore.add(ChatColor.YELLOW + "Total Price: " +
                    ChatColor.GREEN + plugin.getMessageManager().formatPrice(auctionItem.getPriceForRemaining()));
            confirmLore.add(ChatColor.YELLOW + "Price Per Item: " +
                    ChatColor.GREEN + plugin.getMessageManager().formatPrice(auctionItem.getPricePerItem()));
        } else {
            String buyItemText = plugin.getConfig().getString("gui.buttons.confirm_purchase_single", "&aBuy Item");
            confirmMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', buyItemText));
            confirmLore.add("");
            confirmLore.add(ChatColor.YELLOW + "Price: " +
                    ChatColor.GREEN + plugin.getMessageManager().formatPrice(auctionItem.getPriceForRemaining()));
        }

        confirmMeta.setLore(confirmLore);
        confirmButton.setItemMeta(confirmMeta);
        inventory.setItem(11, confirmButton);

        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel Purchase");
        cancelButton.setItemMeta(cancelMeta);
        inventory.setItem(15, cancelButton);

        // Fill remaining slots with filler item
        ItemStack fillerItem = createFillerItem();
        for (int i = 0; i < 27; i++) {
            if (i != 11 && i != 13 && i != 15) {
                inventory.setItem(i, fillerItem);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * Creates an ItemStack for a player head.
     */
    private ItemStack createPlayerHeadItemStack(DatabaseManager.PlayerAuctionInfo playerInfo) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + playerInfo.getName() + "'s Shop");

            // Set the skull owner
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerInfo.getUuid());
            meta.setOwningPlayer(offlinePlayer);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to view " + playerInfo.getName() + "'s auctions");

            // Add item count
            int itemCount = plugin.getAuctionManager().getPlayerAuctionItems(playerInfo.getUuid()).size();
            lore.add(ChatColor.GRAY + "Items for sale: " + ChatColor.WHITE + itemCount);

            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Creates an ItemStack for an auction item.
     * 
     * @param auctionItem The auction item to create an ItemStack for.
     * @param player The player viewing the item, or null if not applicable.
     * @return The created ItemStack.
     */
    private ItemStack createAuctionItemStack(AuctionItem auctionItem, Player player) {
        ItemStack itemStack = auctionItem.getItemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

            // Add auction information to lore
            lore.add("");
            lore.add(ChatColor.YELLOW + "Price: " + ChatColor.GREEN + 
                    plugin.getMessageManager().formatPrice(auctionItem.getPricePerItem()) + " each");
            lore.add(ChatColor.YELLOW + "Quantity: " + ChatColor.WHITE + 
                    auctionItem.getQuantityRemaining() + "/" + auctionItem.getQuantityInitial());
            lore.add("");

            // Show different message based on whether the player is the owner
            if (player != null && auctionItem.getSellerUUID().equals(player.getUniqueId())) {
                lore.add(ChatColor.RED + "Click to cancel auction");
            } else {
                lore.add(ChatColor.GREEN + "Click to purchase");
            }

            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    /**
     * Creates an ItemStack for an auction item (overloaded method for backward compatibility).
     */
    private ItemStack createAuctionItemStack(AuctionItem auctionItem) {
        return createAuctionItemStack(auctionItem, null);
    }

    /**
     * Creates an ItemStack for a mailbox item.
     */
    private ItemStack createMailboxItemStack(MailboxItem mailboxItem) {
        ItemStack itemStack;

        if (mailboxItem.isMoney()) {
            itemStack = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Money: " + 
                    plugin.getMessageManager().formatPrice(mailboxItem.getMoneyAmount()));

            List<String> lore = new ArrayList<>();
            lore.add("");
            if (mailboxItem.getSourceInfo() != null) {
                lore.add(ChatColor.GRAY + mailboxItem.getSourceInfo());
            }
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to claim");

            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        } else {
            ItemStack source = mailboxItem.getItemStack();
            if (source == null) {
                itemStack = new ItemStack(Material.BARRIER);
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "Invalid Item");
                meta.setLore(Collections.singletonList(ChatColor.GRAY + "Item data missing"));
                itemStack.setItemMeta(meta);
            } else {
                itemStack = source.clone();
                ItemMeta meta = itemStack.getItemMeta();

                if (meta != null) {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                    lore.add("");
                    if (mailboxItem.getSourceInfo() != null) {
                        lore.add(ChatColor.GRAY + mailboxItem.getSourceInfo());
                    }
                    lore.add("");
                    lore.add(ChatColor.GREEN + "Click to claim");

                    meta.setLore(lore);
                    itemStack.setItemMeta(meta);
                }
            }
        }

        return itemStack;
    }

    /**
     * Creates a navigation button.
     *
     * @param material The material for the button.
     * @param name The name for the button.
     * @return The created ItemStack.
     */
    private ItemStack createNavigationButton(Material material, String name) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        button.setItemMeta(meta);
        return button;
    }

    /**
     * Creates a filler item for empty slots.
     *
     * @return The created ItemStack.
     */
    private ItemStack createFillerItem() {
        Material fillerMaterial = Material.valueOf(
                plugin.getConfig().getString("gui.filler_item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        return filler;
    }

    /**
     * Handles a player clicking on a player head in the main GUI.
     */
    public boolean handlePlayerHeadClick(Player player, int slot) {
        Paginator<DatabaseManager.PlayerAuctionInfo> paginator = playerHeadPaginators.get(player.getUniqueId());
        if (paginator == null || slot >= ITEMS_PER_PAGE) {
            return false;
        }

        List<DatabaseManager.PlayerAuctionInfo> pageItems = paginator.getCurrentPageItems();
        if (slot >= pageItems.size()) {
            return false;
        }

        DatabaseManager.PlayerAuctionInfo playerInfo = pageItems.get(slot);
        openPlayerItemsGUI(player, playerInfo.getUuid(), playerInfo.getName());
        return true;
    }

    /**
     * Handles a player clicking on an auction item.
     */
    public boolean handleAuctionItemClick(Player player, int slot, boolean isRightClick, boolean isShiftClick) {
        Paginator<AuctionItem> paginator = playerItemPaginators.get(player.getUniqueId());
        if (paginator == null || slot >= ITEMS_PER_PAGE) {
            return false;
        }

        List<AuctionItem> pageItems = paginator.getCurrentPageItems();
        if (slot >= pageItems.size()) {
            return false;
        }

        AuctionItem clickedItem = pageItems.get(slot);

        // Check if player is the owner of this auction
        if (clickedItem.getSellerUUID().equals(player.getUniqueId())) {
            // Player is canceling their own auction
            if (plugin.getAuctionManager().cancelAuction(player, clickedItem.getId())) {
                // Close inventory and open main GUI after successful cancellation
                player.closeInventory();
                // Small delay to ensure smooth transition
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openMainGUI(player);
                }, 1L);
            }
        } else {
            // Player is purchasing from another player
            openPurchaseConfirmGUI(player, clickedItem);
        }

        return true;
    }

    /**
     * Handles a player clicking on a mailbox item.
     */
    public boolean handleMailboxItemClick(Player player, int slot) {
        Paginator<MailboxItem> paginator = mailboxPaginators.get(player.getUniqueId());
        if (paginator == null || slot >= ITEMS_PER_PAGE) {
            return false;
        }

        List<MailboxItem> pageItems = paginator.getCurrentPageItems();
        if (slot >= pageItems.size()) {
            return false;
        }

        MailboxItem clickedItem = pageItems.get(slot);

        // Try to claim the item
        if (plugin.getAuctionManager().claimMailboxItem(player, clickedItem)) {
            // Refresh the mailbox GUI
            openMailboxGUI(player);
        }

        return true;
    }

    /**
     * Handles a player clicking on a navigation button.
     */
    public boolean handleNavigationClick(Player player, int slot, String currentInventoryTitle) {
        if (slot == PREV_PAGE_SLOT) {
            return handlePreviousPageClick(player, currentInventoryTitle);
        } else if (slot == NEXT_PAGE_SLOT) {
            return handleNextPageClick(player, currentInventoryTitle);
        } else if (slot == MAILBOX_SLOT && currentInventoryTitle.contains("Auction House")) {
            openMailboxGUI(player);
            return true;
        } else if (slot == BACK_BUTTON_SLOT) {
            return handleBackButtonClick(player, currentInventoryTitle);
        } else if (slot == MAILBOX_BUTTON_SLOT && currentInventoryTitle.contains("Auction House")) {
            openMailboxGUI(player);
            return true;
        }

        return false;
    }

    private boolean handlePreviousPageClick(Player player, String currentInventoryTitle) {
        if (currentInventoryTitle.contains("Your Mailbox")) {
            Paginator<MailboxItem> paginator = mailboxPaginators.get(player.getUniqueId());
            if (paginator != null && paginator.hasPreviousPage()) {
                paginator.previousPage();
                updateMailboxGUI(player);
                return true;
            }
        } else if (currentInventoryTitle.contains("'s Auctions")) {
            Paginator<AuctionItem> paginator = playerItemPaginators.get(player.getUniqueId());
            if (paginator != null && paginator.hasPreviousPage()) {
                paginator.previousPage();
                String targetName = ChatColor.stripColor(currentInventoryTitle).replace("'s Auctions", "");
                updatePlayerItemsGUI(player, targetName);
                return true;
            }
        } else {
            Paginator<DatabaseManager.PlayerAuctionInfo> paginator = playerHeadPaginators.get(player.getUniqueId());
            if (paginator != null && paginator.hasPreviousPage()) {
                paginator.previousPage();
                updateMainGUI(player, null);
                return true;
            }
        }

        return false;
    }

    private boolean handleNextPageClick(Player player, String currentInventoryTitle) {
        if (currentInventoryTitle.contains("Your Mailbox")) {
            Paginator<MailboxItem> paginator = mailboxPaginators.get(player.getUniqueId());
            if (paginator != null && paginator.hasNextPage()) {
                paginator.nextPage();
                updateMailboxGUI(player);
                return true;
            }
        } else if (currentInventoryTitle.contains("'s Auctions")) {
            Paginator<AuctionItem> paginator = playerItemPaginators.get(player.getUniqueId());
            if (paginator != null && paginator.hasNextPage()) {
                paginator.nextPage();
                String targetName = ChatColor.stripColor(currentInventoryTitle).replace("'s Auctions", "");
                updatePlayerItemsGUI(player, targetName);
                return true;
            }
        } else {
            Paginator<DatabaseManager.PlayerAuctionInfo> paginator = playerHeadPaginators.get(player.getUniqueId());
            if (paginator != null && paginator.hasNextPage()) {
                paginator.nextPage();
                updateMainGUI(player, null);
                return true;
            }
        }

        return false;
    }

    private boolean handleBackButtonClick(Player player, String currentInventoryTitle) {
        if (currentInventoryTitle.contains("'s Auctions") || currentInventoryTitle.contains("Your Mailbox")) {
            openMainGUI(player);
            return true;
        }

        return false;
    }

    /**
     * Handles a player clicking on a purchase confirmation button.
     *
     * @param player The player who clicked.
     * @param slot The slot that was clicked.
     * @param isRightClick Whether the click was a right click.
     * @param isMiddleClick Whether the click was a middle click.
     * @return true if the click was handled, false otherwise.
     */
    public boolean handlePurchaseConfirmClick(Player player, int slot, boolean isRightClick, boolean isMiddleClick) {
        Integer auctionId = pendingPurchases.get(player.getUniqueId());
        if (auctionId == null) {
            return false;
        }

        AuctionItem auctionItem = plugin.getAuctionManager().getAuctionItem(auctionId);
        if (auctionItem == null) {
            player.closeInventory();
            plugin.getMessageManager().sendMessage(player, "auction_not_found");
            pendingPurchases.remove(player.getUniqueId());
            return true;
        }

        if (slot == 11) { // Confirm button
            boolean isStackable = auctionItem.getItemStack().getMaxStackSize() > 1;

            if (!isStackable) {
                // Non-stackable item: always buy the whole item
                player.closeInventory();
                plugin.getAuctionManager().purchaseItem(player, auctionId, auctionItem.getQuantityRemaining());
                pendingPurchases.remove(player.getUniqueId());
            } else if (isMiddleClick) {
                // Middle click - buy specific amount
                player.closeInventory();
                awaitingChatInput.add(player.getUniqueId());
                pendingPurchaseQuantities.put(player.getUniqueId(), auctionItem.getQuantityRemaining());
                plugin.getMessageManager().sendMessage(player, "enter_amount_to_buy");
            } else if (isRightClick) {
                // Right click - buy one
                player.closeInventory();
                plugin.getAuctionManager().purchaseItem(player, auctionId, 1);
                pendingPurchases.remove(player.getUniqueId());
            } else {
                // Left click - buy all
                player.closeInventory();
                plugin.getAuctionManager().purchaseItem(player, auctionId, auctionItem.getQuantityRemaining());
                pendingPurchases.remove(player.getUniqueId());
            }
            return true;
        } else if (slot == 15) { // Cancel button
            player.closeInventory();
            plugin.getMessageManager().sendMessage(player, "purchase_cancelled");

            // Use the existing auctionId variable to determine which player's shop to return to
            if (auctionItem != null) {
                // Return to the player's shop GUI after a short delay
                final AuctionItem finalItem = auctionItem; // Create a final copy for the lambda
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openPlayerItemsGUI(player, finalItem.getSellerUUID(), finalItem.getSellerName());
                }, 1L);
            } else {
                // If we couldn't get the auction item, just return to the main GUI
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openMainGUI(player);
                }, 1L);
            }

            pendingPurchases.remove(player.getUniqueId());
            return true;
        }

        return false;
    }

    /**
     * Handles a player entering a quantity in chat.
     *
     * @param player The player who entered the message.
     * @param message The message entered.
     * @return true if the message was handled, false otherwise.
     */
    public boolean handleChatInput(Player player, String message) {
        if (!awaitingChatInput.contains(player.getUniqueId())) {
            return false;
        }

        awaitingChatInput.remove(player.getUniqueId());

        if (message.equalsIgnoreCase("cancel")) {
            plugin.getMessageManager().sendMessage(player, "purchase_cancelled");

            // Get the auction item to determine which player's shop to return to
            Integer auctionId = pendingPurchases.get(player.getUniqueId());
            pendingPurchases.remove(player.getUniqueId());
            pendingPurchaseQuantities.remove(player.getUniqueId());

            if (auctionId != null) {
                AuctionItem item = plugin.getAuctionManager().getAuctionItem(auctionId);
                if (item != null) {
                    // Return to the player's shop GUI after a short delay
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        openPlayerItemsGUI(player, item.getSellerUUID(), item.getSellerName());
                    }, 1L);
                    return true;
                }
            }

            // If we couldn't get the auction item, just return to the main GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openMainGUI(player);
            }, 1L);
            return true;
        }

        try {
            int quantity = Integer.parseInt(message);
            Integer auctionId = pendingPurchases.get(player.getUniqueId());
            Integer maxQuantity = pendingPurchaseQuantities.get(player.getUniqueId());

            if (auctionId == null || maxQuantity == null) {
                return false;
            }

            if (quantity <= 0) {
                plugin.getMessageManager().sendMessage(player, "must_be_positive_amount");
                return true;
            }

            if (quantity > maxQuantity) {
                plugin.getMessageManager().sendRawMessage(player, "&cThere are only " + maxQuantity + " items available.");
                return true;
            }

            plugin.getAuctionManager().purchaseItem(player, auctionId, quantity);
            pendingPurchases.remove(player.getUniqueId());
            pendingPurchaseQuantities.remove(player.getUniqueId());

        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "invalid_amount_entered");
        }

        return true;
    }

    /**
     * Checks if a player is awaiting chat input.
     *
     * @param playerUUID The UUID of the player to check.
     * @return true if the player is awaiting chat input, false otherwise.
     */
    public boolean isAwaitingChatInput(UUID playerUUID) {
        return awaitingChatInput.contains(playerUUID);
    }

    /**
     * Cleans up data for a player when they quit.
     */
    public void cleanupPlayerData(UUID playerUUID) {
        playerHeadPaginators.remove(playerUUID);
        playerItemPaginators.remove(playerUUID);
        mailboxPaginators.remove(playerUUID);
        pendingPurchases.remove(playerUUID);
        pendingPurchaseQuantities.remove(playerUUID);
        awaitingChatInput.remove(playerUUID);
    }
}
