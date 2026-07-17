package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Locale;
import java.util.Optional;

/**
 * Models the {@code is_eligible} boolean enum carried by the {@code <integrity/>} child of the
 * SMB metered-messaging checkout success reply.
 * <p>
 * The value is the integrity-review verdict on the checkout quote: the compose surface blocks
 * the broadcast send when the value is {@link #FALSE} and admits it when {@link #TRUE}. Only
 * the case-insensitive wire literals {@code "true"} and {@code "false"} are recognised.
 */
@WhatsAppWebModule(moduleName = "WASmaxInSmbMeteredMessagingAccountEnums")
public enum SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility {
    /**
     * Wire form {@code "false"}; the campaign is held back by integrity review and the send action is disabled.
     */
    FALSE,
    /**
     * Wire form {@code "true"}; the campaign has cleared integrity review.
     */
    TRUE;

    /**
     * Resolves a wire-form attribute string into the matching enum constant.
     *
     * @implNote This implementation upper-cases the input under {@link Locale#ROOT} before
     * delegating to {@link #valueOf(String)} and swallows the resulting
     * {@link IllegalArgumentException} as an empty result.
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value
     *         is {@code null} or does not match a documented literal
     */
    public static Optional<SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetSMBMeteredMessagingCheckoutIntegrityEligibility.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
