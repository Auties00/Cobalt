package it.auties.whatsapp.model.business;

import java.util.Arrays;
import java.util.Locale;

/**
 * An enumeration of possible Availabilities.
 */
public enum BusinessItemAvailability {
    /**
     * Indicates an unknown availability.
     */
    UNKNOWN,
    /**
     * Indicates that the item is in stock.
     */
    IN_STOCK,
    /**
     * Indicates that the item is out of stock.
     */
    OUT_OF_STOCK;

    /**
     * Returns an Availability based on the given name.
     *
     * @param name the name of the Availability
     * @return an Availability
     */
    public static BusinessItemAvailability of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.name().toLowerCase(Locale.ROOT).replaceAll("_", " ").equals(name))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
