package pl.dsocraft.auctionhouse.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;
import pl.dsocraft.auctionhouse.utils.ItemSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final DSOAuctionHouse plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(DSOAuctionHouse plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        FileConfiguration config = plugin.getConfig();
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getString("database.host") + ":" +
                config.getInt("database.port") + "/" + config.getString("database.name") +
                "?useSSL=" + config.getBoolean("database.useSSL", false) +
                "&autoReconnect=" + config.getBoolean("database.autoReconnect", true));
        hikariConfig.setUsername(config.getString("database.user"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(10); // Standard value, can be adjusted

        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("Successfully connected to the database!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to the database! Error: " + e.getMessage());
            dataSource = null; // Make sure dataSource is null if connection fails
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            // Try to reconnect if HikariCP hasn't done it automatically
            // or if the connection was closed for some reason.
            // In practice, HikariCP should manage this automatically,
            // but this is an additional safeguard.
            plugin.getLogger().warning("Database connection was closed or null, attempting to reconnect...");
            connect(); // Try to connect again
            if (!isConnected()) { // If still not connected
                 throw new SQLException("Unable to establish a database connection.");
            }
        }
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (isConnected()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }

    private void createTables() {
        if (!isConnected()) {
            plugin.getLogger().severe("Cannot create tables, no database connection.");
            return;
        }
        // Player UUID will be stored as VARCHAR(36)
        // ItemStack will be stored as BLOB after serialization
        // Prices as BIGINT (for large values, e.g., with 'b')

        String createActiveAuctionsTable = "CREATE TABLE IF NOT EXISTS `active_auctions` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY," +
                "`seller_uuid` VARCHAR(36) NOT NULL," +
                "`seller_name` VARCHAR(16) NOT NULL," + // For display and sorting by nickname
                "`item_serialized` BLOB NOT NULL," +
                "`item_name_lowercase` VARCHAR(255) NOT NULL," + // For case-insensitive name search
                "`price_total` BIGINT NOT NULL," +
                "`quantity_initial` INT NOT NULL," +
                "`quantity_remaining` INT NOT NULL," +
                "`listed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX `idx_seller_uuid` (`seller_uuid`)," +
                "INDEX `idx_item_name_lowercase` (`item_name_lowercase`)" +
                ");";

        String createPlayerMailboxTable = "CREATE TABLE IF NOT EXISTS `player_mailbox` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY," +
                "`player_uuid` VARCHAR(36) NOT NULL," +
                "`type` ENUM('ITEM', 'MONEY') NOT NULL," +
                "`item_serialized` BLOB NULL," + // NULL if type is MONEY
                "`money_amount` BIGINT NULL," +  // NULL if type is ITEM
                "`source_info` VARCHAR(255) NULL," + // E.g., "Sold: [Item Name]" or "Purchased: [Item Name]"
                "`added_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX `idx_player_uuid` (`player_uuid`)" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createActiveAuctionsTable);
            stmt.executeUpdate(createPlayerMailboxTable);
            plugin.getLogger().info("Database tables created or already exist.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create database tables! Error: " + e.getMessage(), e);
        }
    }
    /**
     * Gets all mailbox items for a player.
     *
     * @param playerUUID The UUID of the player.
     * @return A list of mailbox items for the player.
     */
    public List<MailboxItem> getPlayerMailboxItems(UUID playerUUID) {
        List<MailboxItem> items = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM player_mailbox WHERE player_uuid = ? ORDER BY added_at DESC")) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(createMailboxItemFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player mailbox items", e);
        }

        return items;
    }

    /**
     * Removes a mailbox item from the database.
     *
     * @param mailboxItemId The ID of the mailbox item to remove.
     * @return true if successful, false otherwise.
     */
    public boolean removeMailboxItem(int mailboxItemId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM player_mailbox WHERE id = ?")) {

            stmt.setInt(1, mailboxItemId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing mailbox item", e);
            return false;
        }
    }

    /**
     * Gets all players who have active auctions, sorted by rank and name.
     *
     * @return A list of player data (UUID, name).
     */
    public List<PlayerAuctionInfo> getPlayersWithAuctions() {
        List<PlayerAuctionInfo> players = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DISTINCT seller_uuid, seller_name FROM active_auctions WHERE quantity_remaining > 0 ORDER BY seller_name")) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("seller_uuid"));
                    String playerName = rs.getString("seller_name");
                    players.add(new PlayerAuctionInfo(playerUUID, playerName));
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting players with auctions", e);
        }

        return players;
    }

    /**
     * Gets players with auctions that match the search term.
     *
     * @param searchTerm The term to search for in item names.
     * @return A list of player data (UUID, name) who have items matching the search.
     */
    public List<PlayerAuctionInfo> getPlayersWithMatchingItems(String searchTerm) {
        List<PlayerAuctionInfo> players = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DISTINCT seller_uuid, seller_name FROM active_auctions " +
                     "WHERE quantity_remaining > 0 AND item_name_lowercase LIKE ? ORDER BY seller_name")) {

            stmt.setString(1, "%" + searchTerm.toLowerCase() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("seller_uuid"));
                    String playerName = rs.getString("seller_name");
                    players.add(new PlayerAuctionInfo(playerUUID, playerName));
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting players with matching items", e);
        }

        return players;
    }

    /**
     * Removes sold out auctions from the database.
     */
    public void cleanupSoldOutAuctions() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM active_auctions WHERE quantity_remaining <= 0")) {

            int removed = stmt.executeUpdate();
            if (removed > 0) {
                plugin.getLogger().info("Cleaned up " + removed + " sold out auctions");
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cleaning up sold out auctions", e);
        }
    }

    /**
     * Creates a MailboxItem object from a ResultSet.
     *
     * @param rs The ResultSet containing mailbox item data.
     * @return The created MailboxItem.
     */
    private MailboxItem createMailboxItemFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
        MailboxItem.Type type = MailboxItem.Type.valueOf(rs.getString("type"));

        ItemStack itemStack = null;
        if (type == MailboxItem.Type.ITEM) {
            byte[] itemData = rs.getBytes("item_serialized");
            if (itemData != null) {
                itemStack = ItemSerializer.deserializeItemStack(itemData);
            }
        }

        long moneyAmount = rs.getLong("money_amount");
        String sourceInfo = rs.getString("source_info");
        long addedAt = rs.getTimestamp("added_at").getTime();

        return new MailboxItem(id, playerUUID, type, itemStack, moneyAmount, sourceInfo, addedAt);
    }

    /**
     * Simple data class to hold player auction information.
     */
    public static class PlayerAuctionInfo {
        private final UUID uuid;
        private final String name;

        public PlayerAuctionInfo(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
