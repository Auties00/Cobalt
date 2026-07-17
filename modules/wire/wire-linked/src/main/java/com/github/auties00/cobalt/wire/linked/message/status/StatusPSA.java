package com.github.auties00.cobalt.wire.linked.message.status;

import java.time.Instant;
import java.util.Objects;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata tag identifying a status as part of a WhatsApp Public Service
 * Announcement (PSA) campaign.
 *
 * <p>WhatsApp occasionally promotes messages from public authorities
 * (health bodies, emergency services, voting information, and similar) as
 * PSAs. Each campaign has a numeric identifier and an expiration instant
 * after which clients should stop highlighting the status as part of the
 * campaign.
 *
 * <p>This class is attached to a status so that clients can group related
 * PSA posts and drop the campaign styling once the campaign has expired.
 */
@ProtobufMessage(name = "StatusPSA")
public final class StatusPSA {
    /**
     * Numeric identifier of the PSA campaign this status belongs to.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.UINT64)
    Long campaignId;

    /**
     * Instant at which the PSA campaign expires.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant campaignExpirationTimestamp;

    /**
     * Constructs a new {@code StatusPSA} with the supplied campaign
     * identifier and expiration instant.
     *
     * <p>The campaign identifier is mandatory and enforced by a
     * {@link Objects#requireNonNull(Object)} check.
     *
     * @param campaignId                  the campaign identifier
     * @param campaignExpirationTimestamp the campaign expiration instant, or {@code null} if absent
     * @throws NullPointerException if {@code campaignId} is {@code null}
     */
    StatusPSA(Long campaignId, Instant campaignExpirationTimestamp) {
        this.campaignId = Objects.requireNonNull(campaignId);
        this.campaignExpirationTimestamp = campaignExpirationTimestamp;
    }

    /**
     * Returns the numeric identifier of the PSA campaign this status
     * belongs to.
     *
     * @return the campaign identifier
     */
    public Long campaignId() {
        return campaignId;
    }

    /**
     * Returns the instant at which the PSA campaign expires.
     *
     * @return the campaign expiration instant, or {@code Optional.empty()} if absent
     */
    public Optional<Instant> campaignExpirationTimestamp() {
        return Optional.ofNullable(campaignExpirationTimestamp);
    }

    /**
     * Sets the numeric identifier of the PSA campaign.
     *
     * @param campaignId the campaign identifier
     */
    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    /**
     * Sets the instant at which the PSA campaign expires.
     *
     * @param campaignExpirationTimestamp the expiration instant, or {@code null} to clear
     */
    public void setCampaignExpirationTimestamp(Instant campaignExpirationTimestamp) {
        this.campaignExpirationTimestamp = campaignExpirationTimestamp;
    }
}
