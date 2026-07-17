package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Optional;

/**
 * A WhatsApp Cloud API commerce settings configuration.
 *
 * <p>Commerce settings govern how a phone number's connected catalog is surfaced in chat: whether the
 * shopping cart is enabled and whether the catalog is visible to consumers. The server-assigned id is
 * present on a read and omitted on a write; both toggles are optional on a write so an update may carry
 * just one of them.
 */
public final class CloudCommerceSettings {
    /**
     * The server-assigned settings id, present on a read and {@code null} on a write.
     */
    private final String id;

    /**
     * Whether the shopping cart is enabled, or {@code null} when unspecified.
     */
    private final Boolean cartEnabled;

    /**
     * Whether the catalog is visible to consumers, or {@code null} when unspecified.
     */
    private final Boolean catalogVisible;

    /**
     * Constructs a new commerce settings configuration.
     *
     * @param id             the server-assigned settings id, or {@code null} on a write
     * @param cartEnabled    whether the shopping cart is enabled, or {@code null} to leave it unspecified
     * @param catalogVisible whether the catalog is visible, or {@code null} to leave it unspecified
     */
    public CloudCommerceSettings(String id, Boolean cartEnabled, Boolean catalogVisible) {
        this.id = id;
        this.cartEnabled = cartEnabled;
        this.catalogVisible = catalogVisible;
    }

    /**
     * Returns the server-assigned settings id.
     *
     * @return an {@link Optional} carrying the id, or empty on a write
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns whether the shopping cart is enabled.
     *
     * @return an {@link Optional} carrying the cart flag, or empty when unspecified
     */
    public Optional<Boolean> cartEnabled() {
        return Optional.ofNullable(cartEnabled);
    }

    /**
     * Returns whether the catalog is visible to consumers.
     *
     * @return an {@link Optional} carrying the visibility flag, or empty when unspecified
     */
    public Optional<Boolean> catalogVisible() {
        return Optional.ofNullable(catalogVisible);
    }
}
