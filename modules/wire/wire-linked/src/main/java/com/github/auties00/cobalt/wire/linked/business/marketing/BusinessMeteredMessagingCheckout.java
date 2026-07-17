package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-issued cost quote for an SMB metered-messaging campaign.
 *
 * <p>The WhatsApp Business client fetches one of these quotes before
 * letting the user dispatch a paid marketing-messages broadcast: the
 * quote tells the user exactly how much the campaign will cost (the
 * {@linkplain #cost() cost projection}), how much of that is already
 * paid via the monthly free-message quota (the optional
 * {@linkplain #quota() quota projection}), and how much of the
 * resulting balance is currently sitting on the account (the
 * {@linkplain #accountBalance() balance projection}). The
 * {@linkplain #integrityEligible() integrity-eligibility} flag tells the
 * client whether the relay's policy engine has cleared the campaign for
 * dispatch — when {@code false}, the campaign must not be queued even
 * if the math works out.
 */
@ProtobufMessage(name = "BusinessMeteredMessagingCheckout")
public final class BusinessMeteredMessagingCheckout {
    /**
     * Mandatory cost breakdown for the campaign.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BusinessMeteredMessagingCost cost;

    /**
     * Whether the relay's policy engine has cleared the campaign for
     * dispatch. {@code false} means the campaign would currently be
     * blocked even if the user had enough credits.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean integrityEligible;

    /**
     * Mandatory account-balance projection.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    BusinessMeteredMessagingAccountBalance accountBalance;

    /**
     * Optional monthly-quota projection (only populated when the
     * relay's free-message quota applies to this campaign).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    BusinessMeteredMessagingQuota quota;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param cost              the cost breakdown
     * @param integrityEligible whether the policy engine cleared the
     *                          campaign
     * @param accountBalance    the account-balance projection
     * @param quota             the optional monthly-quota projection
     */
    BusinessMeteredMessagingCheckout(BusinessMeteredMessagingCost cost,
                                boolean integrityEligible,
                                BusinessMeteredMessagingAccountBalance accountBalance,
                                BusinessMeteredMessagingQuota quota) {
        this.cost = cost;
        this.integrityEligible = integrityEligible;
        this.accountBalance = accountBalance;
        this.quota = quota;
    }

    /**
     * Returns the cost breakdown.
     *
     * @return the cost; never {@code null} for a parsed quote
     */
    public BusinessMeteredMessagingCost cost() {
        return cost;
    }

    /**
     * Returns whether the relay's policy engine has cleared the
     * campaign for dispatch.
     *
     * @return {@code true} when the campaign is eligible, {@code false}
     *         when policy-blocked
     */
    public boolean integrityEligible() {
        return integrityEligible;
    }

    /**
     * Returns the account-balance projection.
     *
     * @return the balance; never {@code null} for a parsed quote
     */
    public BusinessMeteredMessagingAccountBalance accountBalance() {
        return accountBalance;
    }

    /**
     * Returns the optional monthly-quota projection.
     *
     * @return an {@link Optional} carrying the projection, or empty
     *         when the relay omitted it
     */
    public Optional<BusinessMeteredMessagingQuota> quota() {
        return Optional.ofNullable(quota);
    }
}
