package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction.Status;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A model representing a single WhatsApp Business broadcast campaign.
 *
 * <p>A campaign is a scheduled bulk send of a marketing-message template
 * to a {@linkplain BusinessBroadcastList broadcast list} at a specific
 * time. Each campaign carries a stable {@linkplain #id() identifier}, the
 * {@linkplain #deviceId() identifier of the authoring device}, the
 * {@linkplain #adId() click-to-WhatsApp ad identifier} when the campaign
 * was created from an ad, the operator-chosen
 * {@linkplain #name() display name}, the
 * {@linkplain #marketingMessageId() marketing-message identifier} that
 * will be sent, the {@linkplain #broadcastJid() broadcast-list JID} that
 * receives the dispatch, the {@linkplain #reservedQuota() reserved
 * quota}, the {@linkplain #scheduledAt() scheduled} and
 * {@linkplain #createdAt() creation} instants, and the current
 * {@linkplain #status() workflow status} (draft / scheduled / processing
 * / failed / sent).
 *
 * <p>Cobalt persists each campaign independently so callers can resolve a
 * single campaign by id without scanning the entire history.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class BusinessBroadcastCampaign {
    /**
     * The non-{@code null} stable identifier of the campaign. Used as
     * the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The identifier of the device that originally authored this
     * campaign, or {@code null} when no device is known.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer deviceId;

    /**
     * The click-to-WhatsApp ad identifier when the campaign was created
     * from an ad, or {@code null} otherwise.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String adId;

    /**
     * The human-readable display name of the campaign, or {@code null}
     * when no name has been set.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String name;

    /**
     * The identifier of the marketing-message template this campaign
     * sends, or {@code null} when no template is bound yet.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String marketingMessageId;

    /**
     * The JID of the broadcast list receiving the campaign's messages,
     * or {@code null} when no list has been chosen.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    Jid broadcastJid;

    /**
     * The number of delivery quota units reserved for this campaign at
     * scheduling time, or {@code null} when no quota has been reserved.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    Integer reservedQuota;

    /**
     * The instant at which the campaign is scheduled to be dispatched,
     * or {@code null} when the campaign is not scheduled yet.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant scheduledAt;

    /**
     * The instant at which the campaign was originally created, or
     * {@code null} when no creation time is known.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant createdAt;

    /**
     * The current workflow status of the campaign, or {@code null} when
     * no status has been reported.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    Status status;

    /**
     * Constructs a new campaign with the given fields.
     *
     * @param id                 the non-{@code null} campaign identifier
     * @param deviceId           the authoring device identifier, or
     *                           {@code null}
     * @param adId               the click-to-WhatsApp ad identifier, or
     *                           {@code null}
     * @param name               the display name, or {@code null}
     * @param marketingMessageId the bound marketing-message identifier,
     *                           or {@code null}
     * @param broadcastJid       the target broadcast-list JID, or
     *                           {@code null}
     * @param reservedQuota      the reserved quota, or {@code null}
     * @param scheduledAt        the scheduled dispatch instant, or
     *                           {@code null}
     * @param createdAt          the creation instant, or {@code null}
     * @param status             the workflow status, or {@code null}
     */
    BusinessBroadcastCampaign(String id, Integer deviceId, String adId, String name, String marketingMessageId,
                              Jid broadcastJid, Integer reservedQuota, Instant scheduledAt, Instant createdAt,
                              Status status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.deviceId = deviceId;
        this.adId = adId;
        this.name = name;
        this.marketingMessageId = marketingMessageId;
        this.broadcastJid = broadcastJid;
        this.reservedQuota = reservedQuota;
        this.scheduledAt = scheduledAt;
        this.createdAt = createdAt;
        this.status = status;
    }

    /**
     * Returns the non-{@code null} campaign identifier.
     *
     * @return the campaign identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the identifier of the device that authored this campaign.
     *
     * @return an {@code OptionalInt} containing the device identifier,
     *         or empty if not set
     */
    public OptionalInt deviceId() {
        return deviceId == null ? OptionalInt.empty() : OptionalInt.of(deviceId);
    }

    /**
     * Updates the authoring-device identifier.
     *
     * @param deviceId the new device identifier, or {@code null} to
     *                 clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    /**
     * Returns the click-to-WhatsApp ad identifier associated with this
     * campaign.
     *
     * @return an {@code Optional} containing the ad identifier, or empty
     *         when the campaign was not created from an ad
     */
    public Optional<String> adId() {
        return Optional.ofNullable(adId);
    }

    /**
     * Updates the click-to-WhatsApp ad identifier.
     *
     * @param adId the new ad identifier, or {@code null} to clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setAdId(String adId) {
        this.adId = adId;
        return this;
    }

    /**
     * Returns the display name of the campaign.
     *
     * @return an {@code Optional} containing the name, or empty if not
     *         set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Updates the display name of the campaign.
     *
     * @param name the new name, or {@code null} to clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the identifier of the marketing-message template bound to
     * this campaign.
     *
     * @return an {@code Optional} containing the template identifier, or
     *         empty if not set
     */
    public Optional<String> marketingMessageId() {
        return Optional.ofNullable(marketingMessageId);
    }

    /**
     * Updates the bound marketing-message identifier.
     *
     * @param marketingMessageId the new template identifier, or
     *                           {@code null} to clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setMarketingMessageId(String marketingMessageId) {
        this.marketingMessageId = marketingMessageId;
        return this;
    }

    /**
     * Returns the JID of the broadcast list receiving this campaign's
     * messages.
     *
     * @return an {@code Optional} containing the broadcast-list JID, or
     *         empty if not set
     */
    public Optional<Jid> broadcastJid() {
        return Optional.ofNullable(broadcastJid);
    }

    /**
     * Updates the target broadcast-list JID.
     *
     * @param broadcastJid the new broadcast-list JID, or {@code null} to
     *                     clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setBroadcastJid(Jid broadcastJid) {
        this.broadcastJid = broadcastJid;
        return this;
    }

    /**
     * Returns the delivery quota reserved for this campaign.
     *
     * @return an {@code OptionalInt} containing the reserved quota, or
     *         empty if not set
     */
    public OptionalInt reservedQuota() {
        return reservedQuota == null ? OptionalInt.empty() : OptionalInt.of(reservedQuota);
    }

    /**
     * Updates the reserved delivery quota.
     *
     * @param reservedQuota the new reserved quota, or {@code null} to
     *                      clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setReservedQuota(Integer reservedQuota) {
        this.reservedQuota = reservedQuota;
        return this;
    }

    /**
     * Returns the instant at which the campaign is scheduled to be
     * dispatched.
     *
     * @return an {@code Optional} containing the scheduled instant, or
     *         empty if not scheduled
     */
    public Optional<Instant> scheduledAt() {
        return Optional.ofNullable(scheduledAt);
    }

    /**
     * Updates the scheduled dispatch instant.
     *
     * @param scheduledAt the new scheduled instant, or {@code null} to
     *                    clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
        return this;
    }

    /**
     * Returns the instant at which the campaign was created.
     *
     * @return an {@code Optional} containing the creation instant, or
     *         empty if not set
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Updates the creation instant.
     *
     * @param createdAt the new creation instant, or {@code null} to
     *                  clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Returns the current workflow status of the campaign.
     *
     * @return an {@code Optional} containing the status, or empty if not
     *         set
     */
    public Optional<Status> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Updates the workflow status of the campaign.
     *
     * @param status the new status, or {@code null} to clear
     * @return this campaign instance for method chaining
     */
    public BusinessBroadcastCampaign setStatus(Status status) {
        this.status = status;
        return this;
    }

    /**
     * Returns a hash code derived from this campaign's
     * {@linkplain #id() identifier}.
     *
     * @return the hash code of the campaign identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Returns whether this campaign is equal to the given object.
     *
     * <p>Two campaigns are considered equal when they share the same
     * {@linkplain #id() identifier}, regardless of the other fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BusinessBroadcastCampaign}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastCampaign that && Objects.equals(this.id, that.id);
    }
}
