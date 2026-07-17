package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outbound stanza that asks the relay for an SMB metered-messaging
 * checkout projection over the supplied recipient list.
 *
 * <p>This request precomputes the per-broadcast cost projection (price, discounts, account
 * balance, free-quota remaining) shown on the SMB marketing-broadcast confirmation screen before
 * the user commits. It admits 1..2000 participant JIDs and up to 200 pending-campaign entries.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutSmbMeteredMessagingAccountHackBaseIQGetRequestMixin")
public final class SmaxGetSMBMeteredMessagingCheckoutRequest implements SmaxStanza.Request {
    /**
     * The recipient JIDs (1..2000).
     */
    private final List<Jid> participants;

    /**
     * Whether to emit the {@code <use_ad_account/>} marker child.
     */
    private final boolean useAdAccount;

    /**
     * Whether to emit the {@code <skip_dedupe/>} marker child.
     */
    private final boolean skipDedupe;

    /**
     * The optional offer identifier; {@code null} omits the
     * {@code <offer id/>} child.
     */
    private final String offerId;

    /**
     * The optional pending-campaign entries (0..200).
     */
    private final List<SmaxGetSMBMeteredMessagingCheckoutPendingCampaign> pendingCampaigns;

    /**
     * Constructs a request directly.
     *
     * <p>Prefer {@link #builder()} for fluent construction. The relay enforces the 1..2000
     * participant cap and the 0..200 pending-campaign cap; this constructor mirrors both bounds.
     *
     * @param participants     the recipient JIDs; never {@code null}; 1..2000
     * @param useAdAccount     whether to emit the
     *                         {@code <use_ad_account/>} marker
     * @param skipDedupe       whether to emit the
     *                         {@code <skip_dedupe/>} marker
     * @param offerId          the optional offer identifier; may be
     *                         {@code null}
     * @param pendingCampaigns the optional pending campaigns; may be
     *                         {@code null} (treated as empty)
     * @throws NullPointerException     if {@code participants} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code participants} is
     *                                  outside {@code 1..2000} or
     *                                  {@code pendingCampaigns} has
     *                                  more than 200 entries
     */
    public SmaxGetSMBMeteredMessagingCheckoutRequest(List<Jid> participants,
                   boolean useAdAccount,
                   boolean skipDedupe,
                   String offerId,
                   List<SmaxGetSMBMeteredMessagingCheckoutPendingCampaign> pendingCampaigns) {
        Objects.requireNonNull(participants, "participants cannot be null");
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants must contain at least one entry");
        }
        if (participants.size() > 2000) {
            throw new IllegalArgumentException("participants must contain at most 2000 entries");
        }
        var pcCopy = pendingCampaigns == null ? List.<SmaxGetSMBMeteredMessagingCheckoutPendingCampaign>of() : List.copyOf(pendingCampaigns);
        if (pcCopy.size() > 200) {
            throw new IllegalArgumentException("pendingCampaigns must contain at most 200 entries");
        }
        this.participants = List.copyOf(participants);
        this.useAdAccount = useAdAccount;
        this.skipDedupe = skipDedupe;
        this.offerId = offerId;
        this.pendingCampaigns = pcCopy;
    }

    /**
     * Returns a fresh {@link Builder}.
     *
     * <p>This is the canonical entry point. The only mandatory input is at least one participant
     * via {@link Builder#addParticipant(Jid)}; every other setter is optional.
     *
     * @return a new {@link Builder}; never {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the recipient JIDs.
     *
     * <p>These surface as the {@code <to jid/>} children of the outbound {@code <participants/>}
     * payload.
     *
     * @return an unmodifiable list of 1..2000 JIDs
     */
    public List<Jid> participants() {
        return participants;
    }

    /**
     * Returns whether the {@code <use_ad_account/>} marker is set.
     *
     * <p>When {@code true}, the projection bills the user's ad account rather than the standard
     * wallet.
     *
     * @return {@code true} when the marker child will be emitted
     */
    public boolean useAdAccount() {
        return useAdAccount;
    }

    /**
     * Returns whether the {@code <skip_dedupe/>} marker is set.
     *
     * <p>When {@code true}, the projection bypasses the relay's per-recipient deduplication and
     * prices every entry.
     *
     * @return {@code true} when the marker child will be emitted
     */
    public boolean skipDedupe() {
        return skipDedupe;
    }

    /**
     * Returns the optional offer identifier.
     *
     * <p>Returns {@link Optional#empty()} when no offer is being applied. The relay treats the
     * identifier as opaque.
     *
     * @return an {@link Optional} carrying the offer identifier
     */
    public Optional<String> offerId() {
        return Optional.ofNullable(offerId);
    }

    /**
     * Returns the pending-campaign entries.
     *
     * <p>Returns an empty list when none were supplied. Each entry names a campaign that the
     * projection should account for as already-scheduled spend.
     *
     * @return an unmodifiable list of 0..200 entries; never
     *         {@code null}
     */
    public List<SmaxGetSMBMeteredMessagingCheckoutPendingCampaign> pendingCampaigns() {
        return pendingCampaigns;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stamps {@code xmlns="w:biz"} and {@code type="get"}, emits the
     * {@code <participants/>} payload, the optional {@code <use_ad_account/>} and
     * {@code <skip_dedupe/>} markers, the optional {@code <offer id/>} child and the optional
     * {@code <pending_campaigns/>} batch. The IQ {@code id} is assigned by the dispatcher.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutRequest",
            exports = "makeGetSMBMeteredMessagingCheckoutRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        var participantChildren = new ArrayList<Stanza>();
        for (var jid : participants) {
            var toNode = new StanzaBuilder()
                    .description("to")
                    .attribute("jid", jid)
                    .build();
            participantChildren.add(toNode);
        }
        var participantsNode = new StanzaBuilder()
                .description("participants")
                .content(participantChildren)
                .build();
        children.add(participantsNode);
        if (useAdAccount) {
            children.add(new StanzaBuilder().description("use_ad_account").build());
        }
        if (skipDedupe) {
            children.add(new StanzaBuilder().description("skip_dedupe").build());
        }
        if (offerId != null) {
            var offerNode = new StanzaBuilder()
                    .description("offer")
                    .attribute("id", offerId)
                    .build();
            children.add(offerNode);
        }
        if (!pendingCampaigns.isEmpty()) {
            var campaignChildren = new ArrayList<Stanza>();
            for (var entry : pendingCampaigns) {
                campaignChildren.add(entry.toStanza());
            }
            var pendingNode = new StanzaBuilder()
                    .description("pending_campaigns")
                    .content(campaignChildren)
                    .build();
            children.add(pendingNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("type", "get")
                .content(children);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetSMBMeteredMessagingCheckoutRequest) obj;
        return this.useAdAccount == that.useAdAccount
                && this.skipDedupe == that.skipDedupe
                && Objects.equals(this.participants, that.participants)
                && Objects.equals(this.offerId, that.offerId)
                && Objects.equals(this.pendingCampaigns, that.pendingCampaigns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(participants, useAdAccount, skipDedupe, offerId, pendingCampaigns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SmaxGetSMBMeteredMessagingCheckoutRequest[participants=" + participants
                + ", useAdAccount=" + useAdAccount
                + ", skipDedupe=" + skipDedupe
                + ", offerId=" + offerId
                + ", pendingCampaigns=" + pendingCampaigns + ']';
    }

    /**
     * Fluent builder for {@link SmaxGetSMBMeteredMessagingCheckoutRequest}.
     *
     * <p>Accumulates participants and pending-campaign entries incrementally, then materialises a
     * request via {@link #build()}. At least one participant must be added before {@link #build()}
     * is called.
     */
    public static final class Builder {
        /**
         * The accumulating participant list.
         */
        private final List<Jid> participants = new ArrayList<>();

        /**
         * The accumulating pending-campaign list.
         */
        private final List<SmaxGetSMBMeteredMessagingCheckoutPendingCampaign> pendingCampaigns = new ArrayList<>();

        /**
         * The accumulated {@code use_ad_account} toggle.
         */
        private boolean useAdAccount;

        /**
         * The accumulated {@code skip_dedupe} toggle.
         */
        private boolean skipDedupe;

        /**
         * The accumulated offer identifier.
         */
        private String offerId;

        /**
         * Constructs a fresh builder.
         *
         * <p>Prefer {@link SmaxGetSMBMeteredMessagingCheckoutRequest#builder()} as the canonical
         * entry point.
         */
        public Builder() {
        }

        /**
         * Appends a single participant JID.
         *
         * <p>Call once per recipient; the relay enforces a hard cap of 2000 entries at
         * {@link #build()} time.
         *
         * @param jid the recipient JID; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public Builder addParticipant(Jid jid) {
            Objects.requireNonNull(jid, "jid cannot be null");
            this.participants.add(jid);
            return this;
        }

        /**
         * Appends every JID in the supplied list.
         *
         * <p>This is a convenience over {@link #addParticipant(Jid)} for callers that already hold
         * a list.
         *
         * @param entries the JIDs; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code entries} or any
         *                              entry is {@code null}
         */
        public Builder addParticipants(List<Jid> entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            for (var entry : entries) {
                addParticipant(entry);
            }
            return this;
        }

        /**
         * Sets the {@code use_ad_account} toggle.
         *
         * <p>Bills the projection against the user's ad account when {@code true}.
         *
         * @param flag the desired flag value
         * @return this builder
         */
        public Builder useAdAccount(boolean flag) {
            this.useAdAccount = flag;
            return this;
        }

        /**
         * Sets the {@code skip_dedupe} toggle.
         *
         * <p>Bypasses the relay's per-recipient deduplication when {@code true}.
         *
         * @param flag the desired flag value
         * @return this builder
         */
        public Builder skipDedupe(boolean flag) {
            this.skipDedupe = flag;
            return this;
        }

        /**
         * Sets the optional offer identifier.
         *
         * <p>The relay treats the identifier as opaque. Pass {@code null} to clear a previously set
         * value.
         *
         * @param offerId the offer identifier; may be {@code null}
         * @return this builder
         */
        public Builder offerId(String offerId) {
            this.offerId = offerId;
            return this;
        }

        /**
         * Appends a pending-campaign entry.
         *
         * <p>Call once per already-scheduled campaign the projection should account for. The relay
         * enforces a 200-entry cap at {@link #build()} time.
         *
         * @param entry the entry; never {@code null}
         * @return this builder
         * @throws NullPointerException if {@code entry} is
         *                              {@code null}
         */
        public Builder addPendingCampaign(SmaxGetSMBMeteredMessagingCheckoutPendingCampaign entry) {
            Objects.requireNonNull(entry, "entry cannot be null");
            this.pendingCampaigns.add(entry);
            return this;
        }

        /**
         * Materialises a {@link SmaxGetSMBMeteredMessagingCheckoutRequest}.
         *
         * <p>Throws when the accumulated participant list is empty, exceeds 2000 entries, or when
         * more than 200 pending campaigns have been added.
         *
         * @return the constructed request; never {@code null}
         * @throws IllegalArgumentException when participant or
         *                                  pending-campaign caps are
         *                                  exceeded
         */
        public SmaxGetSMBMeteredMessagingCheckoutRequest build() {
            return new SmaxGetSMBMeteredMessagingCheckoutRequest(participants, useAdAccount, skipDedupe, offerId, pendingCampaigns);
        }
    }
}
