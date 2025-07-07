package pl.dsocraft.auctionhouse.utils;

import org.bukkit.inventory.ItemStack;
import java.util.logging.Level;
import pl.dsocraft.auctionhouse.DSOAuctionHouse;

public class ItemSerializer {

    /**
     * Serializes an ItemStack to a byte array using modern Bukkit API.
     *
     * @param itemStack The ItemStack to serialize.
     * @return A byte array representing the ItemStack, or null if itemStack is null.
     */
    public static byte[] serializeItemStack(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        // Since Bukkit 1.16+ (or even earlier for some methods)
        // Definitely available in 1.20
        return itemStack.serializeAsBytes();
    }

    /**
     * Deserializes an ItemStack from a byte array using modern Bukkit API.
     *
     * @param bytes The byte array to deserialize.
     * @return The deserialized ItemStack, or null if bytes are null/empty or deserialization fails.
     */
    public static ItemStack deserializeItemStack(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            // Log the error, as this indicates corrupted data or an issue with deserialization
            DSOAuctionHouse.getInstance().getLogger().log(Level.WARNING, "Failed to deserialize ItemStack", e);
            return null;
        }
    }
}