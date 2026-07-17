package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing the lifecycle status of a single WhatsApp Business
 * broadcast campaign.
 *
 * <p>A campaign is a scheduled bulk send of a marketing message template to a
 * broadcast list (saved audience). The campaign goes through several states
 * server-side, such as {@code DRAFT}, {@code SCHEDULED}, {@code SENDING} or
 * {@code DONE}. This record pairs the campaign's stable
 * {@linkplain #campaignId() identifier} with the latest {@linkplain #status()
 * status string} reported by the server.
 *
 * <p>Cobalt persists each entry independently so callers can query the status
 * of a single campaign without iterating the entire status map. Updates
 * arrive through the corresponding sync action whenever the campaign
 * progresses.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class BusinessCampaignStatus {
    /**
     * The non-{@code null} stable identifier of the broadcast campaign this
     * status refers to. Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String campaignId;

    /**
     * The latest server-reported lifecycle status string for the campaign,
     * or {@code null} when the server has not yet reported a status. Values
     * are opaque strings such as {@code "DRAFT"}, {@code "SCHEDULED"},
     * {@code "SENDING"} or {@code "DONE"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String status;

    /**
     * Constructs a new business campaign status with the given identifier and
     * status string.
     *
     * @param campaignId the non-{@code null} campaign identifier
     * @param status     the lifecycle status string, or {@code null}
     */
    BusinessCampaignStatus(String campaignId, String status) {
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId cannot be null");
        this.status = status;
    }

    /**
     * Returns the non-{@code null} campaign identifier that uniquely identifies
     * this status entry.
     *
     * @return the campaign identifier
     */
    public String campaignId() {
        return campaignId;
    }

    /**
     * Returns the latest server-reported lifecycle status of the campaign.
     *
     * @return an {@code Optional} containing the status string, or empty if
     *         the server has not yet reported a status
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Updates the lifecycle status of the campaign.
     *
     * @param status the new status string, or {@code null} to clear it
     * @return this campaign status instance for method chaining
     */
    public BusinessCampaignStatus setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Returns a hash code derived from this entry's
     * {@linkplain #campaignId() campaign identifier}.
     *
     * @return the hash code of the campaign identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(campaignId);
    }

    /**
     * Returns whether this campaign status is equal to the given object.
     *
     * <p>Two business campaign statuses are considered equal when they share
     * the same {@linkplain #campaignId() campaign identifier}, regardless of
     * their status string.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BusinessCampaignStatus}
     *         with the same campaign identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessCampaignStatus that && Objects.equals(this.campaignId, that.campaignId);
    }
}
