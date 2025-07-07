package pl.dsocraft.auctionhouse.database;

import org.bukkit.inventory.ItemStack;
import pl.dsocraft.auctionhouse.utils.ItemSerializer;

import java.util.UUID;

/**
 * Represents an item listed for auction in the auction house.
 */
public class AuctionItem {
    private final int id;
    private final UUID sellerUUID;
    private final String sellerName;
    private final ItemStack itemStack;
    private final String itemNameLowercase;
    private final long priceTotal;
    private final int quantityInitial;
    private int quantityRemaining;
    private final long listedAt;

    /**
     * Constructor for a new auction item (not yet in the database).
     */
    public AuctionItem(UUID sellerUUID, String sellerName, ItemStack itemStack, 
                      long priceTotal, int quantityInitial) {
        this(-1, sellerUUID, sellerName, itemStack, 
             itemStack.getType().name().toLowerCase(), priceTotal, 
             quantityInitial, quantityInitial, System.currentTimeMillis());
    }

    /**
     * Constructor for an existing auction item (loaded from the database).
     */
    public AuctionItem(int id, UUID sellerUUID, String sellerName, ItemStack itemStack, 
                      String itemNameLowercase, long priceTotal, int quantityInitial, 
                      int quantityRemaining, long listedAt) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.itemNameLowercase = itemNameLowercase;
        this.priceTotal = priceTotal;
        this.quantityInitial = quantityInitial;
        this.quantityRemaining = quantityRemaining;
        this.listedAt = listedAt;
    }

    /**
     * Gets the price per item.
     * @return The price per individual item.
     */
    public long getPricePerItem() {
        return quantityInitial > 0 ? priceTotal / quantityInitial : priceTotal;
    }

    /**
     * Gets the total price for the remaining quantity.
     * @return The total price for the remaining quantity.
     */
    public long getPriceForRemaining() {
        return getPricePerItem() * quantityRemaining;
    }

    /**
     * Decreases the remaining quantity by the specified amount.
     * @param amount The amount to decrease by.
     * @return true if successful, false if the amount is invalid or greater than remaining.
     */
    public boolean decreaseQuantity(int amount) {
        if (amount <= 0 || amount > quantityRemaining) {
            return false;
        }
        quantityRemaining -= amount;
        return true;
    }

    /**
     * Checks if this auction is sold out.
     * @return true if no items remain, false otherwise.
     */
    public boolean isSoldOut() {
        return quantityRemaining <= 0;
    }

    /**
     * Creates a copy of the item with the specified quantity.
     * @param quantity The quantity for the new ItemStack.
     * @return A new ItemStack with the specified quantity.
     */
    public ItemStack createItemStackWithQuantity(int quantity) {
        if (quantity <= 0 || quantity > quantityRemaining) {
            return null;
        }
        ItemStack copy = itemStack.clone();
        copy.setAmount(quantity);
        return copy;
    }

    // Getters

    public int getId() {
        return id;
    }

    public UUID getSellerUUID() {
        return sellerUUID;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItemStack() {
        return itemStack.clone(); // Return a clone to prevent modification
    }

    public String getItemNameLowercase() {
        return itemNameLowercase;
    }

    public long getPriceTotal() {
        return priceTotal;
    }

    public int getQuantityInitial() {
        return quantityInitial;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public long getListedAt() {
        return listedAt;
    }
}