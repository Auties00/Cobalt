package com.github.auties00.cobalt.wire.cloud;

import java.util.OptionalInt;

/**
 * A business-capability change, decoded from a {@code business_capability_update} webhook change.
 *
 * <p>The platform adjusts a business's messaging capacity over time; this update reports the new
 * ceilings.
 */
public final class CloudBusinessCapabilityUpdate {
    /**
     * The maximum daily conversations per phone number, or {@code -1} when not reported.
     */
    private final int maxDailyConversationPerPhone;

    /**
     * The maximum phone numbers per business, or {@code -1} when not reported.
     */
    private final int maxPhoneNumbersPerBusiness;

    /**
     * Constructs a new business-capability update.
     *
     * @param maxDailyConversationPerPhone the maximum daily conversations per phone number, or
     *                                     {@code -1} when not reported
     * @param maxPhoneNumbersPerBusiness   the maximum phone numbers per business, or {@code -1}
     *                                     when not reported
     */
    public CloudBusinessCapabilityUpdate(int maxDailyConversationPerPhone, int maxPhoneNumbersPerBusiness) {
        this.maxDailyConversationPerPhone = maxDailyConversationPerPhone;
        this.maxPhoneNumbersPerBusiness = maxPhoneNumbersPerBusiness;
    }

    /**
     * Returns the maximum daily conversations per phone number.
     *
     * @return an {@link OptionalInt} carrying the ceiling, or empty when not reported
     */
    public OptionalInt maxDailyConversationPerPhone() {
        return maxDailyConversationPerPhone < 0 ? OptionalInt.empty() : OptionalInt.of(maxDailyConversationPerPhone);
    }

    /**
     * Returns the maximum phone numbers per business.
     *
     * @return an {@link OptionalInt} carrying the ceiling, or empty when not reported
     */
    public OptionalInt maxPhoneNumbersPerBusiness() {
        return maxPhoneNumbersPerBusiness < 0 ? OptionalInt.empty() : OptionalInt.of(maxPhoneNumbersPerBusiness);
    }
}
