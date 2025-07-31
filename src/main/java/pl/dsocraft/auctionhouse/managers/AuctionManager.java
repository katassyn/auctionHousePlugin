package pl.dsocraft.auctionhouse.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;
import pl.dsocraft.auctionhouse.database.AuctionItem;
import pl.dsocraft.auctionhouse.database.DatabaseManager;
import pl.dsocraft.auctionhouse.database.MailboxItem;
import pl.dsocraft.auctionhouse.utils.ItemSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages auction-related operations.
 */
public class AuctionManager {

    private final DSOAuctionHouse plugin;
    private final DatabaseManager databaseManager;
    private final Economy economy;
    private LuckPerms luckPerms;

    public AuctionManager(DSOAuctionHouse plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economy = DSOAuctionHouse.getEconomy();

        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            plugin.getLogger().warning("LuckPerms not found or failed to initialize. Permission-based limits will not work.");
        }
    }

    /**
     * Lists an item for auction.
     *
     * @param player The player listing the item.
     * @param itemStack The item to list.
     * @param price The price for the item.
     * @return true if the item was successfully listed, false otherwise.
     */
    public boolean listItem(Player player, ItemStack itemStack, long price) {
        if (itemStack == null || itemStack.getType().isAir()) {
            plugin.getMessageManager().sendMessage(player, "cannot_sell_air");
            return false;
        }

        if (price <= 0) {
            plugin.getMessageManager().sendMessage(player, "must_be_positive_amount");
            return false;
        }

        // Check if player has reached their auction limit
        int playerLimit = getPlayerAuctionLimit(player);
        int currentListings = getPlayerListingsCount(player.getUniqueId());

        if (currentListings >= playerLimit) {
            plugin.getMessageManager().sendMessage(player, "auction_limit_reached", "{limit}", String.valueOf(playerLimit));
            return false;
        }

        // Create a copy of the item to avoid modifying the original
        ItemStack itemToSell = itemStack.clone();
        int quantity = itemToSell.getAmount();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO active_auctions (seller_uuid, seller_name, item_serialized, item_name_lowercase, " +
                     "price_total, quantity_initial, quantity_remaining) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.setBytes(3, ItemSerializer.serializeItemStack(itemToSell));

            // Get item name for searching - clean it for database storage
            String itemName = getCleanItemName(itemToSell);
            stmt.setString(4, itemName.toLowerCase());

            stmt.setLong(5, price);
            stmt.setInt(6, quantity);
            stmt.setInt(7, quantity);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                // Remove the item from the player's inventory
                player.getInventory().removeItem(itemStack);

                // Send success message
                plugin.getMessageManager().sendMessage(player, "item_listed_successfully", 
                        "${price}", plugin.getMessageManager().formatPrice(price));
                return true;
            } else {
                plugin.getMessageManager().sendMessage(player, "error_listing_item");
                return false;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error listing item for auction", e);
            plugin.getMessageManager().sendMessage(player, "error_listing_item");
            return false;
        }
    }

    /**
     * Purchases an item from the auction house.
     *
     * @param player The player making the purchase.
     * @param auctionId The ID of the auction.
     * @param quantity The quantity to purchase.
     * @return true if the purchase was successful, false otherwise.
     */
    public boolean purchaseItem(Player player, int auctionId, int quantity) {
        if (quantity <= 0) {
            plugin.getMessageManager().sendMessage(player, "must_be_positive_amount");
            return false;
        }

        AuctionItem auctionItem = getAuctionItem(auctionId);
        if (auctionItem == null) {
            plugin.getMessageManager().sendMessage(player, "auction_not_found");
            return false;
        }

        // Check if player is trying to buy their own item
        if (auctionItem.getSellerUUID().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendRawMessage(player, "&cYou cannot buy your own items.");
            return false;
        }

        // Check if quantity is valid
        if (quantity > auctionItem.getQuantityRemaining()) {
            plugin.getMessageManager().sendRawMessage(player, "&cThere are only " + 
                    auctionItem.getQuantityRemaining() + " items available.");
            return false;
        }

        // Calculate price for the requested quantity
        long pricePerItem = auctionItem.getPricePerItem();
        long totalPrice = pricePerItem * quantity;

        // Check if player has enough money
        if (!economy.has(player, totalPrice)) {
            plugin.getMessageManager().sendMessage(player, "not_enough_money");
            return false;
        }

        // Process the transaction
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Update the auction item quantity
                if (!updateAuctionItemQuantity(conn, auctionId, quantity)) {
                    throw new SQLException("Failed to update auction item quantity");
                }

                // Withdraw money from buyer
                economy.withdrawPlayer(player, totalPrice);

                // Create a copy of the item with the purchased quantity
                ItemStack purchasedItem = auctionItem.createItemStackWithQuantity(quantity);

                // Add item to buyer's mailbox
                String sourceInfo = "Purchased from " + auctionItem.getSellerName();
                addToMailbox(conn, player.getUniqueId(), MailboxItem.Type.ITEM, purchasedItem, 0, sourceInfo);

                // Add money to seller's mailbox
                OfflinePlayer seller = Bukkit.getOfflinePlayer(auctionItem.getSellerUUID());
                String itemName = purchasedItem.getType().name();
                if (purchasedItem.hasItemMeta() && purchasedItem.getItemMeta().hasDisplayName()) {
                    itemName = purchasedItem.getItemMeta().getDisplayName();
                }
                String sellerSourceInfo = "Sold: " + itemName + " x" + quantity;
                addToMailbox(conn, auctionItem.getSellerUUID(), MailboxItem.Type.MONEY, null, totalPrice, sellerSourceInfo);

                // If the seller is online, notify them
                Player sellerPlayer = Bukkit.getPlayer(auctionItem.getSellerUUID());
                if (sellerPlayer != null && sellerPlayer.isOnline()) {
                    plugin.getMessageManager().sendMessage(sellerPlayer, "item_sold", 
                            "{item_name}", itemName, 
                            "${price}", plugin.getMessageManager().formatPrice(totalPrice));
                }

                // Send success message to buyer
                plugin.getMessageManager().sendMessage(player, "item_purchased", 
                        "{item_name}", itemName, 
                        "${price}", plugin.getMessageManager().formatPrice(totalPrice));

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                plugin.getLogger().log(Level.SEVERE, "Error processing purchase", e);
                plugin.getMessageManager().sendRawMessage(player, "&cAn error occurred while processing your purchase. Please try again.");
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error connecting to database for purchase", e);
            plugin.getMessageManager().sendRawMessage(player, "&cAn error occurred while processing your purchase. Please try again.");
            return false;
        }
    }

    /**
     * Gets an auction item by ID.
     *
     * @param auctionId The ID of the auction.
     * @return The auction item, or null if not found.
     */
    public AuctionItem getAuctionItem(int auctionId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM active_auctions WHERE id = ?")) {

            stmt.setInt(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createAuctionItemFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting auction item", e);
        }

        return null;
    }

    /**
     * Gets all active auction items.
     *
     * @return A list of all active auction items.
     */
    public List<AuctionItem> getAllAuctionItems() {
        List<AuctionItem> items = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM active_auctions WHERE quantity_remaining > 0 ORDER BY listed_at DESC")) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(createAuctionItemFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting all auction items", e);
        }

        return items;
    }

    /**
     * Gets auction items for a specific player.
     *
     * @param playerUUID The UUID of the player.
     * @return A list of auction items for the player.
     */
    public List<AuctionItem> getPlayerAuctionItems(UUID playerUUID) {
        List<AuctionItem> items = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM active_auctions WHERE seller_uuid = ? AND quantity_remaining > 0 ORDER BY listed_at DESC")) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(createAuctionItemFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player auction items", e);
        }

        return items;
    }

    /**
     * Gets players with auctions, sorted by rank and name.
     */
    public List<DatabaseManager.PlayerAuctionInfo> getPlayersWithAuctions() {
        List<DatabaseManager.PlayerAuctionInfo> players = databaseManager.getPlayersWithAuctions();
        return sortPlayersByRank(players);
    }

    /**
     * Gets players with auctions that match the search term.
     */
    public List<DatabaseManager.PlayerAuctionInfo> getPlayersWithMatchingItems(String searchTerm) {
        List<DatabaseManager.PlayerAuctionInfo> players = databaseManager.getPlayersWithMatchingItems(searchTerm);
        return sortPlayersByRank(players);
    }

    /**
     * Sorts players by rank (Deluxe -> Premium -> Default) and then alphabetically.
     */
    private List<DatabaseManager.PlayerAuctionInfo> sortPlayersByRank(List<DatabaseManager.PlayerAuctionInfo> players) {
        if (luckPerms == null) {
            // If LuckPerms is not available, just sort alphabetically
            players.sort(Comparator.comparing(DatabaseManager.PlayerAuctionInfo::getName));
            return players;
        }

        players.sort((p1, p2) -> {
            int rank1 = getPlayerRankPriority(p1.getUuid());
            int rank2 = getPlayerRankPriority(p2.getUuid());

            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2); // Lower number = higher priority
            }

            return p1.getName().compareToIgnoreCase(p2.getName());
        });

        return players;
    }

    /**
     * Gets the rank priority for sorting (lower number = higher priority).
     */
    private int getPlayerRankPriority(UUID playerUUID) {
        if (luckPerms == null) {
            return 3; // Default if LuckPerms not available
        }

        try {
            User user = luckPerms.getUserManager().getUser(playerUUID);
            if (user == null) {
                return 3; // Default
            }

            for (var group : user.getInheritedGroups(user.getQueryOptions())) {
                String groupName = group.getName().toLowerCase();
                switch (groupName) {
                    case "deluxe":
                        return 1;
                    case "premium":
                        return 2;
                    case "default":
                    default:
                        return 3;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking player rank for sorting", e);
        }

        return 3; // Default
    }

    /**
     * Gets the number of active listings for a player.
     *
     * @param playerUUID The UUID of the player.
     * @return The number of active listings.
     */
    public int getPlayerListingsCount(UUID playerUUID) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM active_auctions WHERE seller_uuid = ?")) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player listings count", e);
        }

        return 0;
    }

    /**
     * Gets mailbox items for a player.
     */
    public List<MailboxItem> getPlayerMailboxItems(UUID playerUUID) {
        return databaseManager.getPlayerMailboxItems(playerUUID);
    }

    /**
     * Claims a mailbox item for a player.
     */
    public boolean claimMailboxItem(Player player, MailboxItem mailboxItem) {
        if (mailboxItem.isMoney()) {
            // Give money to player
            economy.depositPlayer(player, mailboxItem.getMoneyAmount());

            // Remove from mailbox
            if (databaseManager.removeMailboxItem(mailboxItem.getId())) {
                plugin.getMessageManager().sendMessage(player, "mailbox_money_claimed", 
                        "${amount}", plugin.getMessageManager().formatPrice(mailboxItem.getMoneyAmount()));
                return true;
            }
        } else if (mailboxItem.isItem()) {
            ItemStack item = mailboxItem.getItemStack();
            if (item != null) {
                // Check if player has space in inventory
                if (player.getInventory().firstEmpty() != -1 || 
                    player.getInventory().containsAtLeast(item, item.getAmount())) {

                    // Give item to player
                    player.getInventory().addItem(item);

                    // Remove from mailbox
                    if (databaseManager.removeMailboxItem(mailboxItem.getId())) {
                        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                                ? item.getItemMeta().getDisplayName() 
                                : item.getType().name();
                        plugin.getMessageManager().sendMessage(player, "mailbox_item_claimed", 
                                "{item_name}", itemName);
                        return true;
                    }
                } else {
                    plugin.getMessageManager().sendMessage(player, "inventory_full_claiming");
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Gets the auction limit for a player based on their permissions.
     *
     * @param player The player to check.
     * @return The auction limit for the player.
     */
    public int getPlayerAuctionLimit(Player player) {
        // Default limit from config
        int defaultLimit = plugin.getConfig().getInt("limits.default", 20);

        if (luckPerms == null) {
            return defaultLimit;
        }

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return defaultLimit;
            }

            // Check for specific group limits in config
            for (var group : user.getInheritedGroups(user.getQueryOptions())) {
                String groupName = group.getName();
                String limitPath = "limits." + groupName.toLowerCase();
                if (plugin.getConfig().contains(limitPath)) {
                    return plugin.getConfig().getInt(limitPath);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking player permissions for auction limit", e);
        }

        return defaultLimit;
    }

    /**
     * Adds an item or money to a player's mailbox.
     *
     * @param conn The database connection.
     * @param playerUUID The UUID of the player.
     * @param type The type of mailbox item.
     * @param itemStack The item to add (null if type is MONEY).
     * @param moneyAmount The amount of money to add (0 if type is ITEM).
     * @param sourceInfo Information about the source of the item/money.
     * @return true if successful, false otherwise.
     */
    private boolean addToMailbox(Connection conn, UUID playerUUID, MailboxItem.Type type,
                                ItemStack itemStack, long moneyAmount, String sourceInfo) throws SQLException {
        sourceInfo = sanitizeSourceInfo(sourceInfo);

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_mailbox (player_uuid, type, item_serialized, money_amount, source_info) " +
                "VALUES (?, ?, ?, ?, ?)")) {

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, type.name());
            stmt.setBytes(3, type == MailboxItem.Type.ITEM ? ItemSerializer.serializeItemStack(itemStack) : null);
            stmt.setLong(4, type == MailboxItem.Type.MONEY ? moneyAmount : 0);
            stmt.setString(5, sourceInfo);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Removes color codes and non-ASCII characters from source info before database insertion.
     */
    private String sanitizeSourceInfo(String sourceInfo) {
        if (sourceInfo == null) {
            return "";
        }
        return ChatColor.stripColor(sourceInfo).replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Updates the quantity of an auction item after a purchase.
     *
     * @param conn The database connection.
     * @param auctionId The ID of the auction.
     * @param quantityPurchased The quantity purchased.
     * @return true if successful, false otherwise.
     */
    private boolean updateAuctionItemQuantity(Connection conn, int auctionId, int quantityPurchased) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE active_auctions SET quantity_remaining = quantity_remaining - ? WHERE id = ?")) {

            stmt.setInt(1, quantityPurchased);
            stmt.setInt(2, auctionId);

            if (stmt.executeUpdate() > 0) {
                // Remove the auction entirely if no items remain
                try (PreparedStatement cleanup = conn.prepareStatement(
                        "DELETE FROM active_auctions WHERE id = ? AND quantity_remaining <= 0")) {
                    cleanup.setInt(1, auctionId);
                    cleanup.executeUpdate();
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Creates an AuctionItem object from a ResultSet.
     *
     * @param rs The ResultSet containing auction item data.
     * @return The created AuctionItem.
     */
    /**
     * Cleans an item name for database storage by removing Unicode characters and color codes.
     * 
     * @param itemStack The item stack to get the name from.
     * @return The cleaned item name.
     */
    private String getCleanItemName(ItemStack itemStack) {
        String itemName = itemStack.getType().name();
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            itemName = itemStack.getItemMeta().getDisplayName();
        }

        // Remove color codes
        itemName = ChatColor.stripColor(itemName);

        // Remove Unicode characters (keep only ASCII)
        itemName = itemName.replaceAll("[^\\x00-\\x7F]", "");

        // Remove any remaining special characters that might cause issues
        itemName = itemName.replaceAll("[^a-zA-Z0-9\\s]", "");

        // Ensure we have at least something to search for
        if (itemName.trim().isEmpty()) {
            itemName = itemStack.getType().name();
        }

        return itemName;
    }

    /**
     * Cancels an auction and returns the item to the player's mailbox.
     * 
     * @param player The player canceling the auction.
     * @param auctionId The ID of the auction to cancel.
     * @return true if the auction was successfully canceled, false otherwise.
     */
    public boolean cancelAuction(Player player, int auctionId) {
        AuctionItem auctionItem = getAuctionItem(auctionId);
        if (auctionItem == null) {
            plugin.getMessageManager().sendMessage(player, "auction_not_found");
            return false;
        }

        // Check if player is the owner of the auction
        if (!auctionItem.getSellerUUID().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendRawMessage(player, "&cYou can only cancel your own auctions.");
            return false;
        }

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Delete the auction from the database
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM active_auctions WHERE id = ?")) {
                    stmt.setInt(1, auctionId);
                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows == 0) {
                        throw new SQLException("Failed to delete auction, no rows affected.");
                    }
                }

                // Create a copy of the item with the remaining quantity
                ItemStack returnedItem = auctionItem.createItemStackWithQuantity(auctionItem.getQuantityRemaining());

                // Add item to player's mailbox
                String sourceInfo = "Canceled auction";
                addToMailbox(conn, player.getUniqueId(), MailboxItem.Type.ITEM, returnedItem, 0, sourceInfo);

                // Send success message
                plugin.getMessageManager().sendRawMessage(player, "&aAuction canceled. The item has been returned to your mailbox.");

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                plugin.getLogger().log(Level.SEVERE, "Error canceling auction", e);
                plugin.getMessageManager().sendRawMessage(player, "&cAn error occurred while canceling your auction. Please try again.");
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error connecting to database for auction cancellation", e);
            plugin.getMessageManager().sendRawMessage(player, "&cAn error occurred while canceling your auction. Please try again.");
            return false;
        }
    }

    private AuctionItem createAuctionItemFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID sellerUUID = UUID.fromString(rs.getString("seller_uuid"));
        String sellerName = rs.getString("seller_name");
        byte[] itemData = rs.getBytes("item_serialized");
        ItemStack itemStack = ItemSerializer.deserializeItemStack(itemData);
        String itemNameLowercase = rs.getString("item_name_lowercase");
        long priceTotal = rs.getLong("price_total");
        int quantityInitial = rs.getInt("quantity_initial");
        int quantityRemaining = rs.getInt("quantity_remaining");
        long listedAt = rs.getTimestamp("listed_at").getTime();

        return new AuctionItem(id, sellerUUID, sellerName, itemStack, itemNameLowercase, 
                              priceTotal, quantityInitial, quantityRemaining, listedAt);
    }
}
