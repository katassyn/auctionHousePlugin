package pl.dsocraft.auctionhouse.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

/**
 * Task that periodically cleans up sold-out auctions from the database.
 */
public class CleanupTask extends BukkitRunnable {

    private final DSOAuctionHouse plugin;

    public CleanupTask(DSOAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Clean up sold-out auctions
        plugin.getDatabaseManager().cleanupSoldOutAuctions();
    }

    /**
     * Starts the cleanup task to run every 30 minutes.
     */
    public void start() {
        // Run every 30 minutes (36000 ticks)
        this.runTaskTimerAsynchronously(plugin, 36000L, 36000L);
    }
}