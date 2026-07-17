package com.github.auties00.cobalt.wire.cloud.waba;

import java.util.Objects;

/**
 * The result of sharing a partner's line of credit with a WhatsApp Business Account.
 *
 * <p>This model carries the allocation configuration the credit-sharing edge creates when a partner
 * attaches its extended credit line to a client account.
 */
public final class CloudCreditAllocation {
    /**
     * The id of the created credit allocation configuration.
     */
    private final String allocationConfigId;

    /**
     * The WhatsApp Business Account id the credit line was shared with.
     */
    private final String wabaId;

    /**
     * Constructs a new credit allocation result.
     *
     * @param allocationConfigId the allocation configuration id
     * @param wabaId             the account id the credit line was shared with
     * @throws NullPointerException if {@code allocationConfigId} or {@code wabaId} is {@code null}
     */
    public CloudCreditAllocation(String allocationConfigId, String wabaId) {
        this.allocationConfigId = Objects.requireNonNull(allocationConfigId, "allocationConfigId must not be null");
        this.wabaId = Objects.requireNonNull(wabaId, "wabaId must not be null");
    }

    /**
     * Returns the id of the created credit allocation configuration.
     *
     * @return the allocation configuration id
     */
    public String allocationConfigId() {
        return allocationConfigId;
    }

    /**
     * Returns the WhatsApp Business Account id the credit line was shared with.
     *
     * @return the account id
     */
    public String wabaId() {
        return wabaId;
    }
}
