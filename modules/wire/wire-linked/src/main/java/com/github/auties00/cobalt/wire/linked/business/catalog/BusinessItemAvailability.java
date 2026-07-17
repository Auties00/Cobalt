package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the stock availability of a product in a WhatsApp Business
 * catalog.
 *
 * <p>Each product in a business catalog has an availability status that
 * determines whether customers can add it to their cart and proceed with
 * a purchase. Products marked as {@link #OUT_OF_STOCK} are displayed
 * differently in the catalog UI and cannot be purchased.
 *
 * <p>The availability can be resolved from its display name (such as
 * {@code "in stock"} or {@code "out of stock"}) using the
 * {@link #ofName(String)} method, which performs case-insensitive matching
 * with underscores replaced by spaces.
 */
@ProtobufEnum
public enum BusinessItemAvailability {
    /**
     * The availability of the product is unknown or has not been specified
     * by the business owner.
     *
     * <p>This is the default value used when the availability field is
     * absent or does not match any recognized value.
     */
    UNKNOWN,

    /**
     * The product is currently in stock and available for purchase.
     *
     * <p>Customers can add this product to their cart and proceed with
     * ordering.
     */
    IN_STOCK,

    /**
     * The product is currently out of stock and unavailable for purchase.
     *
     * <p>Products with this status are still visible in the catalog but
     * cannot be added to a cart. The catalog UI typically displays a
     * visual indicator that the item is unavailable.
     */
    OUT_OF_STOCK;

    /**
     * Lookup map from lowercase display names (with underscores replaced
     * by spaces) to their corresponding enum constants.
     */
    private static final Map<String, BusinessItemAvailability> PRETTY_NAME_TO_AVAILABILITY = Arrays.stream(BusinessItemAvailability.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase().replaceAll("_", " "), Function.identity()));

    /**
     * Returns the availability constant matching the given display name.
     *
     * <p>The lookup is case-insensitive and expects spaces instead of
     * underscores (for example, {@code "in stock"} or
     * {@code "out of stock"}). This matches the format used in catalog
     * query responses.
     *
     * @param name the display name to look up, such as {@code "in stock"}
     * @return an {@code Optional} containing the matching availability
     *         constant, or an empty {@code Optional} if no match is found
     */
    public static Optional<BusinessItemAvailability> ofName(String name) {
        return Optional.ofNullable(PRETTY_NAME_TO_AVAILABILITY.get(name));
    }
}
