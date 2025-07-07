package pl.dsocraft.auctionhouse;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.dsocraft.auctionhouse.commands.*;
import pl.dsocraft.auctionhouse.database.DatabaseManager;
import pl.dsocraft.auctionhouse.managers.AuctionManager;
import pl.dsocraft.auctionhouse.managers.MessageManager;
import pl.dsocraft.auctionhouse.managers.GUIManager;
import pl.dsocraft.auctionhouse.listeners.InventoryClickListener;
import pl.dsocraft.auctionhouse.listeners.PlayerChatListener;
import pl.dsocraft.auctionhouse.tasks.CleanupTask;

import java.util.Objects;
import java.util.logging.Level;

public class DSOAuctionHouse extends JavaPlugin {

    private static DSOAuctionHouse instance;
    private DatabaseManager databaseManager;
    private static Economy econ = null;
    private AuctionManager auctionManager;
    private MessageManager messageManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Copies config.yml if it doesn't exist

        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found or no economy plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.messageManager = new MessageManager(this); // Initialize MessageManager
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.isConnected()) {
            getLogger().severe("Failed to connect to the database. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.auctionManager = new AuctionManager(this);
        this.guiManager = new GUIManager(this);

        registerCommands();
        registerListeners();

        // Start cleanup task
        new CleanupTask(this).start();

        getLogger().info("DSOAuctionHouse has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("DSOAuctionHouse has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private void registerCommands() {
        // Create tab completer
        AuctionTabCompleter tabCompleter = new AuctionTabCompleter(this);

        // Register command executors
        Objects.requireNonNull(getCommand("ah")).setExecutor(new AuctionCommand(this));
        Objects.requireNonNull(getCommand("sell")).setExecutor(new SellCommand(this));
        Objects.requireNonNull(getCommand("checkah")).setExecutor(new CheckCommand(this));
        Objects.requireNonNull(getCommand("ahadmin")).setExecutor(new AdminCommand(this));

        // Register tab completers
        Objects.requireNonNull(getCommand("ah")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("sell")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("checkah")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("ahadmin")).setTabCompleter(tabCompleter);

        // Subcommands for /ah (e.g., find, mailbox) will be managed in AuctionCommand
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
    }

    public static DSOAuctionHouse getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}
