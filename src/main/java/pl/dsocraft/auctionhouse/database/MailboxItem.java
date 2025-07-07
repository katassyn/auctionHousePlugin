package pl.dsocraft.auctionhouse.database;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents an item or money in a player's mailbox.
 */
public class MailboxItem {
    
    public enum Type {
        ITEM,
        MONEY
    }
    
    private final int id;
    private final UUID playerUUID;
    private final Type type;
    private final ItemStack itemStack; // null if type is MONEY
    private final long moneyAmount;    // 0 if type is ITEM
    private final String sourceInfo;
    private final long addedAt;

    /**
     * Constructor for a new mailbox item (not yet in the database).
     */
    public MailboxItem(UUID playerUUID, Type type, ItemStack itemStack, long moneyAmount, String sourceInfo) {
        this(-1, playerUUID, type, itemStack, moneyAmount, sourceInfo, System.currentTimeMillis());
    }

    /**
     * Constructor for an existing mailbox item (loaded from the database).
     */
    public MailboxItem(int id, UUID playerUUID, Type type, ItemStack itemStack, long moneyAmount, 
                      String sourceInfo, long addedAt) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.type = type;
        this.itemStack = itemStack; // Can be null if type is MONEY
        this.moneyAmount = moneyAmount; // Can be 0 if type is ITEM
        this.sourceInfo = sourceInfo;
        this.addedAt = addedAt;
    }

    /**
     * Factory method to create a money mailbox item.
     */
    public static MailboxItem createMoneyMailboxItem(UUID playerUUID, long amount, String sourceInfo) {
        return new MailboxItem(playerUUID, Type.MONEY, null, amount, sourceInfo);
    }

    /**
     * Factory method to create an item mailbox item.
     */
    public static MailboxItem createItemMailboxItem(UUID playerUUID, ItemStack itemStack, String sourceInfo) {
        return new MailboxItem(playerUUID, Type.ITEM, itemStack, 0, sourceInfo);
    }

    /**
     * Checks if this mailbox item contains money.
     */
    public boolean isMoney() {
        return type == Type.MONEY;
    }

    /**
     * Checks if this mailbox item contains an item.
     */
    public boolean isItem() {
        return type == Type.ITEM;
    }

    // Getters

    public int getId() {
        return id;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Type getType() {
        return type;
    }

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null; // Return a clone to prevent modification
    }

    public long getMoneyAmount() {
        return moneyAmount;
    }

    public String getSourceInfo() {
        return sourceInfo;
    }

    public long getAddedAt() {
        return addedAt;
    }
}