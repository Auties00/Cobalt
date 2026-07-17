package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A Cloud API {@code order_details} interactive payment message.
 *
 * <p>This message presents a consumer with an itemised order and a review-and-pay action. It carries
 * the body and optional footer text, an optional header image, a reference id that correlates the
 * order with later {@code order_status} updates, the goods type, the currency, the payable total, the
 * payment configuration, and the order breakdown. The {@link #payment() payment} is a closed union: a
 * {@link CloudOrderPayment.Gateway} carries the generic gateway settings used outside India, and a
 * {@link CloudOrderPayment.India} carries the India-specific configuration name and payment type.
 */
public final class CloudOrderDetailsMessage {
    /**
     * The body text shown above the order.
     */
    private final String bodyText;

    /**
     * The footer text, or {@code null} when none.
     */
    private final String footerText;

    /**
     * The header image link, or {@code null} when no header is shown.
     */
    private final String headerImageLink;

    /**
     * The reference id correlating this order with later status updates.
     */
    private final String referenceId;

    /**
     * The goods type.
     */
    private final CloudGoodsType type;

    /**
     * The ISO-4217 currency code.
     */
    private final String currency;

    /**
     * The total payable amount.
     */
    private final CloudOrderAmount totalAmount;

    /**
     * The payment configuration.
     */
    private final CloudOrderPayment payment;

    /**
     * The order breakdown.
     */
    private final CloudOrder order;

    /**
     * Constructs a new order-details message.
     *
     * @param bodyText        the body text
     * @param footerText      the footer text, or {@code null}
     * @param headerImageLink the header image link, or {@code null}
     * @param referenceId     the reference id
     * @param type            the goods type
     * @param currency        the currency code
     * @param totalAmount     the total payable amount
     * @param payment         the payment configuration
     * @param order           the order breakdown
     * @throws NullPointerException if {@code bodyText}, {@code referenceId}, {@code type},
     *                              {@code currency}, {@code totalAmount}, {@code payment}, or
     *                              {@code order} is {@code null}
     */
    public CloudOrderDetailsMessage(String bodyText, String footerText, String headerImageLink,
                                    String referenceId, CloudGoodsType type, String currency,
                                    CloudOrderAmount totalAmount, CloudOrderPayment payment, CloudOrder order) {
        this.bodyText = Objects.requireNonNull(bodyText, "bodyText must not be null");
        this.footerText = footerText;
        this.headerImageLink = headerImageLink;
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.payment = Objects.requireNonNull(payment, "payment must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
    }

    /**
     * Returns the body text shown above the order.
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
     * Returns the header image link.
     *
     * @return an {@link Optional} carrying the header image link, or empty when no header is shown
     */
    public Optional<String> headerImageLink() {
        return Optional.ofNullable(headerImageLink);
    }

    /**
     * Returns the reference id correlating this order with later status updates.
     *
     * @return the reference id
     */
    public String referenceId() {
        return referenceId;
    }

    /**
     * Returns the goods type.
     *
     * @return the {@link CloudGoodsType}
     */
    public CloudGoodsType type() {
        return type;
    }

    /**
     * Returns the ISO-4217 currency code.
     *
     * @return the currency code
     */
    public String currency() {
        return currency;
    }

    /**
     * Returns the total payable amount.
     *
     * @return the total amount
     */
    public CloudOrderAmount totalAmount() {
        return totalAmount;
    }

    /**
     * Returns the payment configuration.
     *
     * @return the {@link CloudOrderPayment}, either a {@link CloudOrderPayment.Gateway} or a
     *         {@link CloudOrderPayment.India}
     */
    public CloudOrderPayment payment() {
        return payment;
    }

    /**
     * Returns the order breakdown.
     *
     * @return the order
     */
    public CloudOrder order() {
        return order;
    }
}
