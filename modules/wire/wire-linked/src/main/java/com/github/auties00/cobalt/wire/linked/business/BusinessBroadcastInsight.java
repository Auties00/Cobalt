package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A model representing the post-send analytics ("insights") for a single
 * WhatsApp Business broadcast campaign.
 *
 * <p>Once a {@linkplain BusinessBroadcastCampaign campaign} has been
 * sent, the WhatsApp Business backend reports aggregated counters used
 * to render the campaign-report screen. This record pairs the originating
 * campaign {@linkplain #id() identifier} with the latest counts: the
 * total number of {@linkplain #recipientCount() recipients}, the number
 * of {@linkplain #deliveredCount() successful deliveries}, the number of
 * {@linkplain #readCount() reads}, the number of
 * {@linkplain #repliedCount() replies}, and the number of
 * {@linkplain #quickReplyCount() quick replies} triggered by the
 * campaign.
 *
 * <p>Cobalt persists each insight record independently so callers can
 * pull up the report for a single campaign without iterating the whole
 * insights store.
 *
 * <p>This class is a local model only. Modifying its fields does not send
 * any request to the WhatsApp servers; it simply reflects the locally
 * cached state.
 */
@ProtobufMessage
public final class BusinessBroadcastInsight {
    /**
     * The non-{@code null} campaign identifier these insights describe.
     * Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The total number of recipients targeted by the campaign, or
     * {@code null} when no count has been reported.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer recipientCount;

    /**
     * The number of recipients who successfully received the campaign
     * message, or {@code null} when no count has been reported.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer deliveredCount;

    /**
     * The number of recipients who read the campaign message, or
     * {@code null} when no count has been reported.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    Integer readCount;

    /**
     * The number of recipients who replied to the campaign message, or
     * {@code null} when no count has been reported.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    Integer repliedCount;

    /**
     * The number of quick replies triggered by the campaign message, or
     * {@code null} when no count has been reported.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    Integer quickReplyCount;

    /**
     * Constructs a new insight record with the given campaign identifier
     * and counters.
     *
     * @param id              the non-{@code null} campaign identifier
     * @param recipientCount  the total recipient count, or {@code null}
     * @param deliveredCount  the delivered count, or {@code null}
     * @param readCount       the read count, or {@code null}
     * @param repliedCount    the replied count, or {@code null}
     * @param quickReplyCount the quick-reply count, or {@code null}
     */
    BusinessBroadcastInsight(String id, Integer recipientCount, Integer deliveredCount,
                             Integer readCount, Integer repliedCount, Integer quickReplyCount) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.recipientCount = recipientCount;
        this.deliveredCount = deliveredCount;
        this.readCount = readCount;
        this.repliedCount = repliedCount;
        this.quickReplyCount = quickReplyCount;
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
     * Returns the total number of recipients targeted by the campaign.
     *
     * @return an {@code OptionalInt} containing the count, or empty if
     *         not reported
     */
    public OptionalInt recipientCount() {
        return recipientCount == null ? OptionalInt.empty() : OptionalInt.of(recipientCount);
    }

    /**
     * Updates the total recipient count.
     *
     * @param recipientCount the new count, or {@code null} to clear
     * @return this insight instance for method chaining
     */
    public BusinessBroadcastInsight setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
        return this;
    }

    /**
     * Returns the number of recipients who successfully received the
     * campaign message.
     *
     * @return an {@code OptionalInt} containing the count, or empty if
     *         not reported
     */
    public OptionalInt deliveredCount() {
        return deliveredCount == null ? OptionalInt.empty() : OptionalInt.of(deliveredCount);
    }

    /**
     * Updates the delivered count.
     *
     * @param deliveredCount the new count, or {@code null} to clear
     * @return this insight instance for method chaining
     */
    public BusinessBroadcastInsight setDeliveredCount(Integer deliveredCount) {
        this.deliveredCount = deliveredCount;
        return this;
    }

    /**
     * Returns the number of recipients who read the campaign message.
     *
     * @return an {@code OptionalInt} containing the count, or empty if
     *         not reported
     */
    public OptionalInt readCount() {
        return readCount == null ? OptionalInt.empty() : OptionalInt.of(readCount);
    }

    /**
     * Updates the read count.
     *
     * @param readCount the new count, or {@code null} to clear
     * @return this insight instance for method chaining
     */
    public BusinessBroadcastInsight setReadCount(Integer readCount) {
        this.readCount = readCount;
        return this;
    }

    /**
     * Returns the number of recipients who replied to the campaign
     * message.
     *
     * @return an {@code OptionalInt} containing the count, or empty if
     *         not reported
     */
    public OptionalInt repliedCount() {
        return repliedCount == null ? OptionalInt.empty() : OptionalInt.of(repliedCount);
    }

    /**
     * Updates the replied count.
     *
     * @param repliedCount the new count, or {@code null} to clear
     * @return this insight instance for method chaining
     */
    public BusinessBroadcastInsight setRepliedCount(Integer repliedCount) {
        this.repliedCount = repliedCount;
        return this;
    }

    /**
     * Returns the number of quick replies triggered by the campaign
     * message.
     *
     * @return an {@code OptionalInt} containing the count, or empty if
     *         not reported
     */
    public OptionalInt quickReplyCount() {
        return quickReplyCount == null ? OptionalInt.empty() : OptionalInt.of(quickReplyCount);
    }

    /**
     * Updates the quick-reply count.
     *
     * @param quickReplyCount the new count, or {@code null} to clear
     * @return this insight instance for method chaining
     */
    public BusinessBroadcastInsight setQuickReplyCount(Integer quickReplyCount) {
        this.quickReplyCount = quickReplyCount;
        return this;
    }

    /**
     * Returns a hash code derived from this insight's
     * {@linkplain #id() campaign identifier}.
     *
     * @return the hash code of the campaign identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Returns whether this insight is equal to the given object.
     *
     * <p>Two insights are considered equal when they describe the same
     * {@linkplain #id() campaign identifier}, regardless of their counts.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BusinessBroadcastInsight}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastInsight that && Objects.equals(this.id, that.id);
    }
}
