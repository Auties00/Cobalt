package com.github.auties00.cobalt.wire.cloud;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes how an outbound message is charged.
 *
 * <p>The platform reports how a sent message is billed once a conversation or per-message billing
 * decision has been made. The information rides the status transition that carries the billing
 * decision and is absent on transitions that carry none. A pricing record states whether the message
 * is billable, the pricing model that governs the charge, the pricing type that qualifies it, and the
 * conversation category that priced it.
 */
public final class CloudMessagePricing {
    /**
     * Whether the message is billable.
     *
     * <p>WhatsApp defaults this to {@code true} for a regular {@link #pricingType() pricing type};
     * free entry-point and free customer-service windows report {@code false}.
     */
    private final boolean billable;

    /**
     * The pricing model that governs the charge.
     *
     * <p>The known values are {@code CBP} (conversation-based pricing, charged per 24-hour
     * conversation window) and {@code PMP} (per-message pricing, charged per delivered message). The
     * value is kept as a {@link String} so an unrecognised model reported by a newer API version
     * still decodes.
     */
    private final String pricingModel;

    /**
     * The pricing type that qualifies the charge, or {@code null} when not reported.
     *
     * <p>The known values are {@code regular}, {@code free_entry_point}, and
     * {@code free_customer_service}; further values may be introduced by newer API versions. The
     * member is reported under the {@code type} key, or under the legacy {@code pricing_type} key.
     * The value is kept as a {@link String} for forward compatibility.
     */
    private final String pricingType;

    /**
     * The conversation category that priced the charge.
     *
     * <p>The known values include {@code marketing}, {@code utility}, {@code authentication},
     * {@code service}, {@code authentication_international}, {@code referral_conversion}, and
     * {@code marketing_lite}, among others; newer API versions may report additional categories. The
     * value is kept as a {@link String} so an unrecognised category still decodes.
     */
    private final String category;

    /**
     * Constructs a new message pricing record.
     *
     * @param billable     whether the message is billable
     * @param pricingModel the pricing model, for example {@code CBP} or {@code PMP}
     * @param pricingType  the pricing type, for example {@code regular}, or {@code null}
     * @param category     the conversation category, for example {@code marketing}
     * @throws NullPointerException if {@code pricingModel} or {@code category} is {@code null}
     */
    public CloudMessagePricing(boolean billable, String pricingModel, String pricingType, String category) {
        this.billable = billable;
        this.pricingModel = Objects.requireNonNull(pricingModel, "pricingModel must not be null");
        this.pricingType = pricingType;
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    /**
     * Returns whether the message is billable.
     *
     * @return {@code true} when the message is billable
     */
    public boolean billable() {
        return billable;
    }

    /**
     * Returns the pricing model that governs the charge.
     *
     * @return the pricing model, for example {@code CBP} or {@code PMP}
     */
    public String pricingModel() {
        return pricingModel;
    }

    /**
     * Returns the pricing type that qualifies the charge.
     *
     * @return an {@link Optional} carrying the pricing type, for example {@code regular}, or empty
     *         when not reported
     */
    public Optional<String> pricingType() {
        return Optional.ofNullable(pricingType);
    }

    /**
     * Returns the conversation category that priced the charge.
     *
     * @return the conversation category, for example {@code marketing}
     */
    public String category() {
        return category;
    }
}
