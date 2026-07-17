package com.github.auties00.cobalt.wire.linked.business.marketing;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.queryMeteredMessagingCheckout}.
 * Carries the campaign recipients together with the billing and
 * deduplication knobs that shape the metered-messaging quote.
 *
 * <p>{@link #participants} is required; the remaining fields are optional.
 * {@link #useAdAccount} selects whether the quote is billed against the
 * linked ad account, {@link #skipDedupe} disables recipient
 * deduplication, {@link #offerId} pins the quote to a specific offer, and
 * {@link #pendingCampaigns} declares already-drafted campaigns whose
 * reserved free messages must be deducted before the new charge is
 * computed.
 */
@ProtobufMessage
public final class BusinessMeteredMessagingCheckoutRequest {
    /**
     * The recipients the metered-messaging campaign would be sent to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<Jid> participants;

    /**
     * Whether to bill the campaign against the linked ad account.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean useAdAccount;

    /**
     * Whether to skip recipient deduplication.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean skipDedupe;

    /**
     * The optional offer id the quote is pinned to, or {@code null}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String offerId;

    /**
     * The optional list of already-drafted pending campaigns (at most
     * 200) whose reserved free messages are deducted from the quote.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<BusinessMeteredMessagingPendingCampaign> pendingCampaigns;

    /**
     * Constructs a new {@code BusinessMeteredMessagingCheckoutRequest}.
     *
     * @param participants     the campaign recipients; required
     * @param useAdAccount     whether to bill against the linked ad account
     * @param skipDedupe       whether to skip recipient deduplication
     * @param offerId          the optional offer id, or {@code null}
     * @param pendingCampaigns the optional pending-campaign list, or {@code null}
     * @throws NullPointerException if {@code participants} is {@code null}
     */
    BusinessMeteredMessagingCheckoutRequest(List<Jid> participants, boolean useAdAccount, boolean skipDedupe,
                                            String offerId, List<BusinessMeteredMessagingPendingCampaign> pendingCampaigns) {
        this.participants = Objects.requireNonNull(participants, "participants cannot be null");
        this.useAdAccount = useAdAccount;
        this.skipDedupe = skipDedupe;
        this.offerId = offerId;
        this.pendingCampaigns = pendingCampaigns;
    }

    /**
     * Returns the campaign recipients.
     *
     * @return the recipients; never {@code null}
     */
    public List<Jid> participants() {
        return participants;
    }

    /**
     * Returns whether the quote is billed against the linked ad account.
     *
     * @return {@code true} to bill against the ad account
     */
    public boolean useAdAccount() {
        return useAdAccount;
    }

    /**
     * Returns whether recipient deduplication is skipped.
     *
     * @return {@code true} to skip deduplication
     */
    public boolean skipDedupe() {
        return skipDedupe;
    }

    /**
     * Returns the offer id the quote is pinned to.
     *
     * @return an {@link Optional} carrying the offer id, or empty when unset
     */
    public Optional<String> offerId() {
        return Optional.ofNullable(offerId);
    }

    /**
     * Returns the declared pending campaigns.
     *
     * @return an unmodifiable list of pending campaigns; never {@code null},
     *         possibly empty
     */
    public List<BusinessMeteredMessagingPendingCampaign> pendingCampaigns() {
        return pendingCampaigns == null ? List.of() : pendingCampaigns;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessMeteredMessagingCheckoutRequest) obj;
        return useAdAccount == that.useAdAccount &&
                skipDedupe == that.skipDedupe &&
                Objects.equals(participants, that.participants) &&
                Objects.equals(offerId, that.offerId) &&
                Objects.equals(pendingCampaigns, that.pendingCampaigns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participants, useAdAccount, skipDedupe, offerId, pendingCampaigns);
    }

    @Override
    public String toString() {
        return "BusinessMeteredMessagingCheckoutRequest[" +
                "participants=" + participants + ", " +
                "useAdAccount=" + useAdAccount + ", " +
                "skipDedupe=" + skipDedupe + ", " +
                "offerId=" + offerId + ", " +
                "pendingCampaigns=" + pendingCampaigns + ']';
    }
}
