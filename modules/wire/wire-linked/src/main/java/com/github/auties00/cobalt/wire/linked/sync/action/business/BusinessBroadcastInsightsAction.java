package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * A sync action carrying aggregated delivery insights for a business broadcast campaign.
 *
 * <p>After a campaign has been dispatched, the server computes engagement statistics and
 * sends them back through this action so that every linked device can show the same
 * counters in the business UI. The record tracks how many recipients received the
 * campaign, how many messages were delivered, read, and replied to, and how many quick
 * replies were triggered.
 *
 * <p>This action is transported in the {@code REGULAR} sync collection and keyed by the
 * campaign identifier through {@link BusinessBroadcastInsightsActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.BusinessBroadcastInsightsAction")
public final class BusinessBroadcastInsightsAction implements SyncAction<BusinessBroadcastInsightsActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "business_broadcast_insights_sync";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * The total number of recipients that were targeted by the campaign.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    Integer recipientCount;

    /**
     * The number of recipients to whom the campaign message was successfully delivered.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer deliveredCount;

    /**
     * The number of recipients who read the delivered campaign message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer readCount;

    /**
     * The number of recipients who replied to the campaign message.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    Integer repliedCount;

    /**
     * The number of quick replies triggered by the campaign message.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    Integer quickReplyCount;

    /**
     * Creates an insights action with the supplied counters.
     *
     * @param recipientCount  the total number of recipients, or {@code null} if not set
     * @param deliveredCount  the number of successful deliveries, or {@code null} if not set
     * @param readCount       the number of recipients who read the message, or {@code null} if not set
     * @param repliedCount    the number of recipients who replied, or {@code null} if not set
     * @param quickReplyCount the number of quick replies, or {@code null} if not set
     */
    BusinessBroadcastInsightsAction(Integer recipientCount, Integer deliveredCount, Integer readCount, Integer repliedCount, Integer quickReplyCount) {
        this.recipientCount = recipientCount;
        this.deliveredCount = deliveredCount;
        this.readCount = readCount;
        this.repliedCount = repliedCount;
        this.quickReplyCount = quickReplyCount;
    }

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
     * Returns the total number of recipients targeted by the campaign.
     *
     * @return the recipient count, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt recipientCount() {
        return recipientCount == null ? OptionalInt.empty() : OptionalInt.of(recipientCount);
    }

    /**
     * Returns the number of recipients who successfully received the campaign message.
     *
     * @return the delivered count, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt deliveredCount() {
        return deliveredCount == null ? OptionalInt.empty() : OptionalInt.of(deliveredCount);
    }

    /**
     * Returns the number of recipients who read the campaign message.
     *
     * @return the read count, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt readCount() {
        return readCount == null ? OptionalInt.empty() : OptionalInt.of(readCount);
    }

    /**
     * Returns the number of recipients who replied to the campaign message.
     *
     * @return the replied count, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt repliedCount() {
        return repliedCount == null ? OptionalInt.empty() : OptionalInt.of(repliedCount);
    }

    /**
     * Returns the number of quick replies triggered by the campaign message.
     *
     * @return the quick reply count, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt quickReplyCount() {
        return quickReplyCount == null ? OptionalInt.empty() : OptionalInt.of(quickReplyCount);
    }
}
