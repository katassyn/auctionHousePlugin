package pl.dsocraft.auctionhouse.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceParser {

    // Pattern to match a number and optional modifier (k, m, b)
    // Allows decimal numbers like 1.5k
    private static final Pattern PRICE_PATTERN = Pattern.compile("^(\\d*\\.?\\d+)([kmb]?)$", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a price string (e.g., "100", "50k", "2.5m", "1b") into a long value.
     *
     * @param priceString The string representation of the price.
     * @return The parsed price as a long, or -1 if the format is invalid or value is non-positive.
     */
    public static long parsePrice(String priceString) {
        if (priceString == null || priceString.trim().isEmpty()) {
            return -1;
        }

        Matcher matcher = PRICE_PATTERN.matcher(priceString.trim());
        if (!matcher.matches()) {
            return -1; // Doesn't match the pattern
        }

        try {
            double value = Double.parseDouble(matcher.group(1));
            String modifier = matcher.group(2).toLowerCase();

            if (value <= 0) {
                return -1; // Price must be positive
            }

            switch (modifier) {
                case "k":
                    value *= 1_000;
                    break;
                case "m":
                    value *= 1_000_000;
                    break;
                case "b":
                    value *= 1_000_000_000;
                    break;
                case "": // No modifier
                    break;
                default:
                    return -1; // Unknown modifier (though the pattern should catch this)
            }

            if (value > Long.MAX_VALUE) { // Check for long overflow
                return -2; // Special error code for value too large
            }

            return (long) Math.floor(value); // Use Math.floor to truncate the fractional part after multiplication
                                          // e.g., 1.53k -> 1530. Could also use Math.round() if preferred
        } catch (NumberFormatException e) {
            return -1; // Error parsing the number
        }
    }
}