package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A sync action describing a business broadcast campaign, which is a scheduled delivery of
 * a marketing message to the members of a broadcast list.
 *
 * <p>Campaigns are created in the business tooling and then replicated to every linked device
 * through this action so that campaign state (draft, scheduled, processing, failed, sent) stays
 * consistent across surfaces. The record carries the campaign metadata (name, advertising
 * identifier, associated message id, target broadcast list JID), scheduling information
 * (reserved quota, scheduled and creation timestamps), the current workflow status, and the
 * device identifier of the device that originally authored the campaign.
 */
@ProtobufMessage(name = "SyncActionValue.BusinessBroadcastCampaignAction")
public final class BusinessBroadcastCampaignAction implements SyncAction<BusinessBroadcastCampaignActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "business_broadcast_campaign";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The identifier of the device that created the campaign, used to attribute authorship
     * across linked devices.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    Integer deviceId;

    /**
     * The identifier of the click-to-WhatsApp ad, when the campaign was created from an ad.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String adId;

    /**
     * The human-readable name of the campaign as shown in the business UI.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String name;

    /**
     * The identifier of the marketing message that this campaign sends.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String msgId;

    /**
     * The JID of the broadcast list that receives the campaign's messages.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String broadcastJid;

    /**
     * The number of delivery quota units reserved for this campaign at scheduling time.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    Integer reservedQuota;

    /**
     * The timestamp, in milliseconds, at which the campaign is scheduled to be dispatched.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    Long scheduledTimestamp;

    /**
     * The timestamp, in milliseconds, at which the campaign was originally created.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64)
    Long createTimestamp;

    /**
     * The current workflow status of the campaign.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    Status status;


    /**
     * Creates a campaign action with all fields populated.
     *
     * @param deviceId           the identifier of the authoring device
     * @param adId               the click-to-WhatsApp ad identifier, or {@code null}
     * @param name               the campaign display name, or {@code null}
     * @param msgId              the marketing message identifier, or {@code null}
     * @param broadcastJid       the JID of the targeted broadcast list, or {@code null}
     * @param reservedQuota      the reserved delivery quota, or {@code null}
     * @param scheduledTimestamp the scheduled dispatch timestamp in milliseconds, or {@code null}
     * @param createTimestamp    the creation timestamp in milliseconds, or {@code null}
     * @param status             the campaign workflow status, or {@code null}
     */
    BusinessBroadcastCampaignAction(Integer deviceId, String adId, String name, String msgId, String broadcastJid, Integer reservedQuota, Long scheduledTimestamp, Long createTimestamp, Status status) {
        this.deviceId = deviceId;
        this.adId = adId;
        this.name = name;
        this.msgId = msgId;
        this.broadcastJid = broadcastJid;
        this.reservedQuota = reservedQuota;
        this.scheduledTimestamp = scheduledTimestamp;
        this.createTimestamp = createTimestamp;
        this.status = status;
    }

    /**
     * Returns the identifier of the device that authored this campaign.
     *
     * @return the device identifier, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt deviceId() {
        return deviceId == null ? OptionalInt.empty() : OptionalInt.of(deviceId);
    }

    /**
     * Returns the click-to-WhatsApp ad identifier associated with this campaign.
     *
     * @return the ad identifier, or {@link Optional#empty()} if the campaign was not created from an ad
     */
    public Optional<String> adId() {
        return Optional.ofNullable(adId);
    }

    /**
     * Returns the display name of this campaign.
     *
     * @return the campaign name, or {@link Optional#empty()} if not set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the identifier of the marketing message sent by this campaign.
     *
     * @return the marketing message identifier, or {@link Optional#empty()} if not set
     */
    public Optional<String> msgId() {
        return Optional.ofNullable(msgId);
    }

    /**
     * Returns the JID of the broadcast list that receives this campaign.
     *
     * @return the broadcast list JID, or {@link Optional#empty()} if not set
     */
    public Optional<String> broadcastJid() {
        return Optional.ofNullable(broadcastJid);
    }

    /**
     * Returns the delivery quota reserved for this campaign.
     *
     * @return the reserved quota, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt reservedQuota() {
        return reservedQuota == null ? OptionalInt.empty() : OptionalInt.of(reservedQuota);
    }

    /**
     * Returns the time at which the campaign is scheduled to be dispatched, in milliseconds.
     *
     * @return the scheduled timestamp, or {@link OptionalLong#empty()} if not set
     */
    public OptionalLong scheduledTimestamp() {
        return scheduledTimestamp == null ? OptionalLong.empty() : OptionalLong.of(scheduledTimestamp);
    }

    /**
     * Returns the time at which the campaign was created, in milliseconds.
     *
     * @return the creation timestamp, or {@link OptionalLong#empty()} if not set
     */
    public OptionalLong createTimestamp() {
        return createTimestamp == null ? OptionalLong.empty() : OptionalLong.of(createTimestamp);
    }

    /**
     * Returns the current workflow status of the campaign.
     *
     * @return the status, or {@link Optional#empty()} if not set
     */
    public Optional<Status> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Updates the authoring device identifier.
     *
     * @param deviceId the new device identifier, or {@code null} to clear it
     */
    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Updates the click-to-WhatsApp ad identifier.
     *
     * @param adId the new ad identifier, or {@code null} to clear it
     */
    public void setAdId(String adId) {
        this.adId = adId;
    }

    /**
     * Updates the campaign display name.
     *
     * @param name the new name, or {@code null} to clear it
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Updates the marketing message identifier.
     *
     * @param msgId the new marketing message identifier, or {@code null} to clear it
     */
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    /**
     * Updates the JID of the targeted broadcast list.
     *
     * @param broadcastJid the new broadcast list JID, or {@code null} to clear it
     */
    public void setBroadcastJid(String broadcastJid) {
        this.broadcastJid = broadcastJid;
    }

    /**
     * Updates the reserved delivery quota.
     *
     * @param reservedQuota the new reserved quota, or {@code null} to clear it
     */
    public void setReservedQuota(Integer reservedQuota) {
        this.reservedQuota = reservedQuota;
    }

    /**
     * Updates the scheduled dispatch timestamp.
     *
     * @param scheduledTimestamp the new scheduled timestamp in milliseconds, or {@code null} to clear it
     */
    public void setScheduledTimestamp(Long scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    /**
     * Updates the creation timestamp.
     *
     * @param createTimestamp the new creation timestamp in milliseconds, or {@code null} to clear it
     */
    public void setCreateTimestamp(Long createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    /**
     * Updates the current workflow status of the campaign.
     *
     * @param status the new status, or {@code null} to clear it
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * The workflow status of a {@link BusinessBroadcastCampaignAction}, mirroring the lifecycle
     * that a campaign progresses through inside the business tooling.
     */
    @ProtobufEnum(name = "SyncActionValue.BusinessBroadcastCampaignAction.Status")
    public static enum Status {
        /**
         * The campaign has been saved but not yet submitted for delivery.
         */
        DRAFT(1),
        /**
         * The campaign has been scheduled and is waiting for its dispatch time.
         */
        SCHEDULED(2),
        /**
         * The server is currently dispatching the campaign messages.
         */
        PROCESSING(3),
        /**
         * The campaign could not be dispatched and has been marked as failed.
         */
        FAILED(4),
        /**
         * The campaign has finished dispatching all of its messages.
         */
        SENT(5);

        /**
         * Creates a status constant with the supplied protobuf wire index.
         *
         * @param index the protobuf wire index of this constant
         */
        Status(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this status constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this status constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
