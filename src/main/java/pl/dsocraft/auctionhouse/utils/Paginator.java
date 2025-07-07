package pl.dsocraft.auctionhouse.utils;

import java.util.List;

/**
 * Utility class for handling pagination in GUIs.
 */
public class Paginator<T> {
    private final List<T> items;
    private final int itemsPerPage;
    private int currentPage;

    /**
     * Creates a new paginator with the specified items and items per page.
     *
     * @param items The list of items to paginate.
     * @param itemsPerPage The number of items to display per page.
     */
    public Paginator(List<T> items, int itemsPerPage) {
        this.items = items;
        this.itemsPerPage = Math.max(1, itemsPerPage); // Ensure at least 1 item per page
        this.currentPage = 0; // 0-based indexing for pages
    }

    /**
     * Gets the total number of pages.
     *
     * @return The total number of pages.
     */
    public int getTotalPages() {
        if (items.isEmpty()) {
            return 1; // At least one page, even if empty
        }
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    /**
     * Gets the current page number (1-based for display).
     *
     * @return The current page number.
     */
    public int getCurrentPageNumber() {
        return currentPage + 1; // Convert to 1-based for display
    }

    /**
     * Gets the items for the current page.
     *
     * @return A sublist of items for the current page.
     */
    public List<T> getCurrentPageItems() {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        if (startIndex >= items.size()) {
            // If current page is beyond the available items (e.g., after items were removed),
            // go to the last page
            goToLastPage();
            startIndex = currentPage * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, items.size());
        }
        
        return items.subList(startIndex, endIndex);
    }

    /**
     * Checks if there is a next page.
     *
     * @return true if there is a next page, false otherwise.
     */
    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }

    /**
     * Checks if there is a previous page.
     *
     * @return true if there is a previous page, false otherwise.
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Goes to the next page if available.
     *
     * @return true if successfully moved to the next page, false if already at the last page.
     */
    public boolean nextPage() {
        if (hasNextPage()) {
            currentPage++;
            return true;
        }
        return false;
    }

    /**
     * Goes to the previous page if available.
     *
     * @return true if successfully moved to the previous page, false if already at the first page.
     */
    public boolean previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            return true;
        }
        return false;
    }

    /**
     * Goes to the first page.
     */
    public void goToFirstPage() {
        currentPage = 0;
    }

    /**
     * Goes to the last page.
     */
    public void goToLastPage() {
        currentPage = Math.max(0, getTotalPages() - 1);
    }

    /**
     * Goes to the specified page if valid.
     *
     * @param page The page number (0-based).
     * @return true if successfully moved to the specified page, false if the page is invalid.
     */
    public boolean goToPage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            currentPage = page;
            return true;
        }
        return false;
    }

    /**
     * Gets the total number of items.
     *
     * @return The total number of items.
     */
    public int getTotalItems() {
        return items.size();
    }

    /**
     * Gets the number of items per page.
     *
     * @return The number of items per page.
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }
}