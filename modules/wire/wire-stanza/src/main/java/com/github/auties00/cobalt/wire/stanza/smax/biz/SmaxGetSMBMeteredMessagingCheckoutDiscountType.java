package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Models the {@code type} enum carried by the {@code <discount/>} grandchildren of the
 * {@code <discounts/>} child in the SMB metered-messaging checkout success reply.
 * <p>
 * The value classifies each discount line on the checkout quote: {@link #FREEMSG} delivers a
 * count of free messages granted to the campaign while {@link #PERCENTAGE} is a percentage off
 * the per-message rate surfaced via the sibling {@code percentage} attribute. Only the
 * documented wire literals are recognised.
 */
@WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountEnums")
public enum SmaxGetSMBMeteredMessagingCheckoutDiscountType {
    /**
     * Free-message discount line; the discount is a number of free messages granted to the campaign.
     */
    FREEMSG,
    /**
     * Percentage-off discount line; the discount is a percentage off the per-message rate.
     */
    PERCENTAGE;

    /**
     * Resolves a wire-form attribute string into the matching enum constant.
     *
     * @implNote This implementation matches the wire literals directly because the
     * {@code free_msg} value carries an underscore that {@link String#toUpperCase} preserves,
     * so a naive {@link #valueOf(String)} dispatch on the upper-cased form would never match
     * the {@link #FREEMSG} constant.
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value
     *         is {@code null} or does not match a documented literal
     */
    public static Optional<SmaxGetSMBMeteredMessagingCheckoutDiscountType> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value) {
            case "free_msg" -> Optional.of(FREEMSG);
            case "percentage" -> Optional.of(PERCENTAGE);
            default -> Optional.empty();
        };
    }
}
