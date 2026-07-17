package com.github.auties00.cobalt.wam.threadlogging;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;

/**
 * A mutable per-(chat, day) tally of thread-interaction counters staged for the once-per-day
 * {@code ThreadInteractionData} WAM upload.
 *
 * <p>WhatsApp Web aggregates each thread's daily activity incrementally: as messages, reactions,
 * forwards, edits, view-once interactions, calls, and commerce exchanges occur, the matching counter
 * on the row for the thread's current day bucket is bumped in place. The row is held in the WAM
 * sub-store until its day has fully elapsed, at which point it is enriched with live chat metadata,
 * serialized into the family of {@code ThreadInteractionData} events, and deleted. This message is the
 * persisted shape of one such row.
 *
 * <p>{@link #chatJid()} carries the legacy JID string identifying the thread and {@link #startTs()}
 * carries the day-bucket start instant; together they form the row's identity. Every other
 * field is a non-negative running count that defaults to {@code 0} and is mutated through the fluent
 * setters, each of which returns this same instance so the producer can bump a counter with
 * {@code row.setMessagesSent(row.messagesSent() + 1)}.
 *
 * @see ThreadLoggingActivity
 * @see ThreadLoggingService
 */
@ProtobufMessage
@WhatsAppWebModule(moduleName = "WAWebChatThreadLogging")
@SuppressWarnings("UnusedReturnValue")
public final class ThreadLoggingCounters {
    /**
     * The legacy JID string identifying the thread this row aggregates.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String chatJid;

    /**
     * The start of the day bucket this row aggregates, as produced by the thread-logging day-bucket
     * computation; serialized on the wire as an epoch-seconds {@code UINT64} via
     * {@link InstantSecondsMixin}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    final Instant startTs;

    /**
     * The number of non-reaction, non-edit messages sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    long messagesSent;

    /**
     * The number of non-reaction messages received in this thread during the day bucket.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    long messagesReceived;

    /**
     * The number of messages marked read in this thread during the day bucket.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    long messagesRead;

    /**
     * The number of reactions sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64)
    long reactionsSent;

    /**
     * The number of reactions received in this thread during the day bucket.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    long reactionsReceived;

    /**
     * The number of forwarded messages sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64)
    long forwardMessagesSent;

    /**
     * The number of forwarded messages received in this thread during the day bucket.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64)
    long forwardMessagesReceived;

    /**
     * The number of message edits sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    long editedMessagesSent;

    /**
     * The number of view-once messages sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.INT64)
    long viewOnceMessagesSent;

    /**
     * The number of view-once messages received in this thread during the day bucket.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT64)
    long viewOnceMessagesReceived;

    /**
     * The number of view-once messages opened in this thread during the day bucket.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.INT64)
    long viewOnceMessagesOpened;

    /**
     * The number of replies sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.INT64)
    long repliesSent;

    /**
     * The number of outgoing call offers placed in this thread during the day bucket.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.INT64)
    long callOffersSent;

    /**
     * The number of incoming call offers received in this thread during the day bucket.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.INT64)
    long callOffersReceived;

    /**
     * The total connected call duration, in seconds, accumulated in this thread during the day bucket.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.INT64)
    long totalCallDuration;

    /**
     * The number of commerce messages sent in this thread during the day bucket.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.INT64)
    long commerceMessagesSent;

    /**
     * The number of commerce messages received in this thread during the day bucket.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.INT64)
    long commerceMessagesReceived;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param chatJid                  the legacy JID string identifying the thread
     * @param startTs                  the day-bucket start instant
     * @param messagesSent             the messages-sent count
     * @param messagesReceived         the messages-received count
     * @param messagesRead             the messages-read count
     * @param reactionsSent            the reactions-sent count
     * @param reactionsReceived        the reactions-received count
     * @param forwardMessagesSent      the forwarded-messages-sent count
     * @param forwardMessagesReceived  the forwarded-messages-received count
     * @param editedMessagesSent       the edited-messages-sent count
     * @param viewOnceMessagesSent     the view-once-messages-sent count
     * @param viewOnceMessagesReceived the view-once-messages-received count
     * @param viewOnceMessagesOpened   the view-once-messages-opened count
     * @param repliesSent              the replies-sent count
     * @param callOffersSent           the call-offers-sent count
     * @param callOffersReceived       the call-offers-received count
     * @param totalCallDuration        the total connected call duration in seconds
     * @param commerceMessagesSent     the commerce-messages-sent count
     * @param commerceMessagesReceived the commerce-messages-received count
     */
    ThreadLoggingCounters(String chatJid, Instant startTs, long messagesSent, long messagesReceived,
                          long messagesRead, long reactionsSent, long reactionsReceived,
                          long forwardMessagesSent, long forwardMessagesReceived, long editedMessagesSent,
                          long viewOnceMessagesSent, long viewOnceMessagesReceived, long viewOnceMessagesOpened,
                          long repliesSent, long callOffersSent, long callOffersReceived, long totalCallDuration,
                          long commerceMessagesSent, long commerceMessagesReceived) {
        this.chatJid = chatJid;
        this.startTs = startTs;
        this.messagesSent = messagesSent;
        this.messagesReceived = messagesReceived;
        this.messagesRead = messagesRead;
        this.reactionsSent = reactionsSent;
        this.reactionsReceived = reactionsReceived;
        this.forwardMessagesSent = forwardMessagesSent;
        this.forwardMessagesReceived = forwardMessagesReceived;
        this.editedMessagesSent = editedMessagesSent;
        this.viewOnceMessagesSent = viewOnceMessagesSent;
        this.viewOnceMessagesReceived = viewOnceMessagesReceived;
        this.viewOnceMessagesOpened = viewOnceMessagesOpened;
        this.repliesSent = repliesSent;
        this.callOffersSent = callOffersSent;
        this.callOffersReceived = callOffersReceived;
        this.totalCallDuration = totalCallDuration;
        this.commerceMessagesSent = commerceMessagesSent;
        this.commerceMessagesReceived = commerceMessagesReceived;
    }

    /**
     * Returns the legacy JID string identifying the thread this row aggregates.
     *
     * @return the legacy JID string
     */
    public String chatJid() {
        return chatJid;
    }

    /**
     * Returns the start of the day bucket this row aggregates.
     *
     * @return the day-bucket start instant
     */
    public Instant startTs() {
        return startTs;
    }

    /**
     * Returns the messages-sent count.
     *
     * @return the messages-sent count
     */
    public long messagesSent() {
        return messagesSent;
    }

    /**
     * Sets the messages-sent count.
     *
     * @param messagesSent the new messages-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setMessagesSent(long messagesSent) {
        this.messagesSent = messagesSent;
        return this;
    }

    /**
     * Returns the messages-received count.
     *
     * @return the messages-received count
     */
    public long messagesReceived() {
        return messagesReceived;
    }

    /**
     * Sets the messages-received count.
     *
     * @param messagesReceived the new messages-received count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setMessagesReceived(long messagesReceived) {
        this.messagesReceived = messagesReceived;
        return this;
    }

    /**
     * Returns the messages-read count.
     *
     * @return the messages-read count
     */
    public long messagesRead() {
        return messagesRead;
    }

    /**
     * Sets the messages-read count.
     *
     * @param messagesRead the new messages-read count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setMessagesRead(long messagesRead) {
        this.messagesRead = messagesRead;
        return this;
    }

    /**
     * Returns the reactions-sent count.
     *
     * @return the reactions-sent count
     */
    public long reactionsSent() {
        return reactionsSent;
    }

    /**
     * Sets the reactions-sent count.
     *
     * @param reactionsSent the new reactions-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setReactionsSent(long reactionsSent) {
        this.reactionsSent = reactionsSent;
        return this;
    }

    /**
     * Returns the reactions-received count.
     *
     * @return the reactions-received count
     */
    public long reactionsReceived() {
        return reactionsReceived;
    }

    /**
     * Sets the reactions-received count.
     *
     * @param reactionsReceived the new reactions-received count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setReactionsReceived(long reactionsReceived) {
        this.reactionsReceived = reactionsReceived;
        return this;
    }

    /**
     * Returns the forwarded-messages-sent count.
     *
     * @return the forwarded-messages-sent count
     */
    public long forwardMessagesSent() {
        return forwardMessagesSent;
    }

    /**
     * Sets the forwarded-messages-sent count.
     *
     * @param forwardMessagesSent the new forwarded-messages-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setForwardMessagesSent(long forwardMessagesSent) {
        this.forwardMessagesSent = forwardMessagesSent;
        return this;
    }

    /**
     * Returns the forwarded-messages-received count.
     *
     * @return the forwarded-messages-received count
     */
    public long forwardMessagesReceived() {
        return forwardMessagesReceived;
    }

    /**
     * Sets the forwarded-messages-received count.
     *
     * @param forwardMessagesReceived the new forwarded-messages-received count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setForwardMessagesReceived(long forwardMessagesReceived) {
        this.forwardMessagesReceived = forwardMessagesReceived;
        return this;
    }

    /**
     * Returns the edited-messages-sent count.
     *
     * @return the edited-messages-sent count
     */
    public long editedMessagesSent() {
        return editedMessagesSent;
    }

    /**
     * Sets the edited-messages-sent count.
     *
     * @param editedMessagesSent the new edited-messages-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setEditedMessagesSent(long editedMessagesSent) {
        this.editedMessagesSent = editedMessagesSent;
        return this;
    }

    /**
     * Returns the view-once-messages-sent count.
     *
     * @return the view-once-messages-sent count
     */
    public long viewOnceMessagesSent() {
        return viewOnceMessagesSent;
    }

    /**
     * Sets the view-once-messages-sent count.
     *
     * @param viewOnceMessagesSent the new view-once-messages-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setViewOnceMessagesSent(long viewOnceMessagesSent) {
        this.viewOnceMessagesSent = viewOnceMessagesSent;
        return this;
    }

    /**
     * Returns the view-once-messages-received count.
     *
     * @return the view-once-messages-received count
     */
    public long viewOnceMessagesReceived() {
        return viewOnceMessagesReceived;
    }

    /**
     * Sets the view-once-messages-received count.
     *
     * @param viewOnceMessagesReceived the new view-once-messages-received count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setViewOnceMessagesReceived(long viewOnceMessagesReceived) {
        this.viewOnceMessagesReceived = viewOnceMessagesReceived;
        return this;
    }

    /**
     * Returns the view-once-messages-opened count.
     *
     * @return the view-once-messages-opened count
     */
    public long viewOnceMessagesOpened() {
        return viewOnceMessagesOpened;
    }

    /**
     * Sets the view-once-messages-opened count.
     *
     * @param viewOnceMessagesOpened the new view-once-messages-opened count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setViewOnceMessagesOpened(long viewOnceMessagesOpened) {
        this.viewOnceMessagesOpened = viewOnceMessagesOpened;
        return this;
    }

    /**
     * Returns the replies-sent count.
     *
     * @return the replies-sent count
     */
    public long repliesSent() {
        return repliesSent;
    }

    /**
     * Sets the replies-sent count.
     *
     * @param repliesSent the new replies-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setRepliesSent(long repliesSent) {
        this.repliesSent = repliesSent;
        return this;
    }

    /**
     * Returns the call-offers-sent count.
     *
     * @return the call-offers-sent count
     */
    public long callOffersSent() {
        return callOffersSent;
    }

    /**
     * Sets the call-offers-sent count.
     *
     * @param callOffersSent the new call-offers-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setCallOffersSent(long callOffersSent) {
        this.callOffersSent = callOffersSent;
        return this;
    }

    /**
     * Returns the call-offers-received count.
     *
     * @return the call-offers-received count
     */
    public long callOffersReceived() {
        return callOffersReceived;
    }

    /**
     * Sets the call-offers-received count.
     *
     * @param callOffersReceived the new call-offers-received count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setCallOffersReceived(long callOffersReceived) {
        this.callOffersReceived = callOffersReceived;
        return this;
    }

    /**
     * Returns the total connected call duration, in seconds.
     *
     * @return the total connected call duration in seconds
     */
    public long totalCallDuration() {
        return totalCallDuration;
    }

    /**
     * Sets the total connected call duration, in seconds.
     *
     * @param totalCallDuration the new total connected call duration in seconds
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setTotalCallDuration(long totalCallDuration) {
        this.totalCallDuration = totalCallDuration;
        return this;
    }

    /**
     * Returns the commerce-messages-sent count.
     *
     * @return the commerce-messages-sent count
     */
    public long commerceMessagesSent() {
        return commerceMessagesSent;
    }

    /**
     * Sets the commerce-messages-sent count.
     *
     * @param commerceMessagesSent the new commerce-messages-sent count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setCommerceMessagesSent(long commerceMessagesSent) {
        this.commerceMessagesSent = commerceMessagesSent;
        return this;
    }

    /**
     * Returns the commerce-messages-received count.
     *
     * @return the commerce-messages-received count
     */
    public long commerceMessagesReceived() {
        return commerceMessagesReceived;
    }

    /**
     * Sets the commerce-messages-received count.
     *
     * @param commerceMessagesReceived the new commerce-messages-received count
     * @return this instance for method chaining
     */
    public ThreadLoggingCounters setCommerceMessagesReceived(long commerceMessagesReceived) {
        this.commerceMessagesReceived = commerceMessagesReceived;
        return this;
    }
}
