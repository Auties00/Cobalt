package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A Cloud API {@code order_status} interactive payment message.
 *
 * <p>This message updates a consumer on the progress of an order previously sent as an
 * {@code order_details} message, correlated by the same reference id. The {@link CloudOrderStatus
 * status} reports the order's progress; the optional description adds free-form context.
 */
public final class CloudOrderStatusMessage {
    /**
     * The body text shown above the status.
     */
    private final String bodyText;

    /**
     * The footer text, or {@code null} when none.
     */
    private final String footerText;

    /**
     * The reference id of the order being updated.
     */
    private final String referenceId;

    /**
     * The new order status.
     */
    private final CloudOrderStatus status;

    /**
     * The free-form status description, or {@code null} when none.
     */
    private final String description;

    /**
     * Constructs a new order-status message.
     *
     * @param bodyText    the body text
     * @param footerText  the footer text, or {@code null}
     * @param referenceId the reference id of the order being updated
     * @param status      the new order status
     * @param description the status description, or {@code null}
     * @throws NullPointerException if {@code bodyText}, {@code referenceId}, or {@code status} is
     *                              {@code null}
     */
    public CloudOrderStatusMessage(String bodyText, String footerText, String referenceId,
                                   CloudOrderStatus status, String description) {
        this.bodyText = Objects.requireNonNull(bodyText, "bodyText must not be null");
        this.footerText = footerText;
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.description = description;
    }

    /**
     * Returns the body text shown above the status.
     *
     * @return the body text
     */
    public String bodyText() {
        return bodyText;
    }

    /**
     * Returns the footer text.
     *
     * @return an {@link Optional} carrying the footer text, or empty when none
     */
    public Optional<String> footerText() {
        return Optional.ofNullable(footerText);
    }

    /**
     * Returns the reference id of the order being updated.
     *
     * @return the reference id
     */
    public String referenceId() {
        return referenceId;
    }

    /**
     * Returns the new order status.
     *
     * @return the {@link CloudOrderStatus}
     */
    public CloudOrderStatus status() {
        return status;
    }

    /**
     * Returns the free-form status description.
     *
     * @return an {@link Optional} carrying the description, or empty when none
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }
}
