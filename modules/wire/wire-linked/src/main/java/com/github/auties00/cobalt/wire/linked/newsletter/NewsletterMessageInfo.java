package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.linked.message.*;
import com.github.auties00.cobalt.wire.core.message.*;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a single message published inside a newsletter.
 *
 * <p>Unlike chat messages, newsletter messages are not end-to-end
 * encrypted: the server broadcasts them as plaintext protobuf payloads
 * decoded against the standard message specification. To compensate for
 * the lack of privacy, the server never reveals individual reactor or
 * voter identities; subscribers only see aggregated tallies.
 *
 * <p>Each instance bundles the shared message envelope inherited from
 * {@link LinkedMessageInfo} (key, timestamp, content, delivery status, starred
 * flag, receipts) with newsletter-specific state:
 * <ul>
 *   <li>the server-assigned monotonic identifier used to order messages</li>
 *   <li>the view count, forwards count, and question-response count</li>
 *   <li>the aggregated reactions keyed by emoji</li>
 *   <li>the aggregated poll vote tallies</li>
 *   <li>timestamps tracking the last server update, the original send
 *       time, and the most recent edit</li>
 *   <li>the admin profile that published the message</li>
 *   <li>a WAMO subscription flag marking paid-content messages</li>
 *   <li>an optional media handle used when the message carries an upload</li>
 * </ul>
 */
@ProtobufMessage
public final class NewsletterMessageInfo implements LinkedMessageInfo {
    /**
     * The immutable key identifying this message (remote JID, client id,
     * sender).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The monotonic identifier assigned by the newsletter server, used to
     * order messages within the newsletter.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    int serverId;

    /**
     * The moment at which the message was originally delivered.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * The number of subscribers that have viewed this message. {@code null}
     * when not reported yet.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    Long views;

    /**
     * The aggregated reaction tallies, keyed by their emoji content.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    Map<String, NewsletterReaction> reactions;

    /**
     * The container holding the message content. May be {@code null} for
     * placeholder or deleted messages.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    LinkedMessageContainer message;

    /**
     * The delivery status of this message. {@code null} when the server
     * has not yet reported one.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    MessageStatus status;

    /**
     * Whether the current viewer has starred this message.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    boolean starred;

    /**
     * Delivery receipts associated with this message.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    List<MessageReceipt> receipts;

    /**
     * The number of times this message has been forwarded. {@code null}
     * when not reported yet.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT64)
    Long forwardsCount;

    /**
     * The number of responses received when the message is a question
     * post. {@code null} when the message is not a question or no
     * responses exist.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.UINT64)
    Long questionResponsesCount;

    /**
     * The most recent moment at which the server reported updated
     * aggregates for this message.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant lastUpdateFromServerTimestamp;

    /**
     * The sender-side timestamp, in milliseconds, of the most recent edit.
     * {@code null} when the message has never been edited.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.UINT64)
    Long latestEditSenderTimestampMs;

    /**
     * The original send timestamp before any edits were applied.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant originalTimestamp;

    /**
     * Whether this message is gated behind a WAMO subscription.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    boolean wamoSub;

    /**
     * The admin profile that published this message, when admin-profile
     * attribution is used.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    NewsletterAdminProfile adminProfile;

    /**
     * The aggregated poll vote tallies for poll messages.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    List<NewsletterPollVote> pollVotes;

    /**
     * A transient handle produced by the media upload pipeline for
     * messages that carry attachments. Not persisted via protobuf.
     */
    String mediaHandle;

    /**
     * Constructs a new {@code NewsletterMessageInfo}. Invoked by the
     * generated protobuf deserializer.
     *
     * @param key                            the message key, must not be {@code null}
     * @param serverId                       the server-assigned monotonic identifier
     * @param timestamp                      the original send timestamp, may be {@code null}
     * @param views                          the view count, may be {@code null}
     * @param reactions                      the aggregated reactions keyed by emoji, may be {@code null}
     * @param message                        the message content container, may be {@code null}
     * @param status                         the delivery status, may be {@code null}
     * @param starred                        whether the message is starred
     * @param receipts                       the delivery receipts, may be {@code null}
     * @param forwardsCount                  the forwards count, may be {@code null}
     * @param questionResponsesCount         the number of question responses, may be {@code null}
     * @param lastUpdateFromServerTimestamp  the timestamp of the last aggregate update, may be {@code null}
     * @param latestEditSenderTimestampMs    the latest edit timestamp in milliseconds, may be {@code null}
     * @param originalTimestamp              the original send timestamp before edits, may be {@code null}
     * @param wamoSub                        whether the message is WAMO-gated
     * @param adminProfile                   the publishing admin profile, may be {@code null}
     * @param pollVotes                      the poll vote tallies, defaulted to an empty mutable list when {@code null}
     * @throws NullPointerException          if {@code key} is {@code null}
     */
    NewsletterMessageInfo(MessageKey key, int serverId, Instant timestamp, Long views, Map<String, NewsletterReaction> reactions, LinkedMessageContainer message, MessageStatus status, boolean starred, List<MessageReceipt> receipts, Long forwardsCount, Long questionResponsesCount, Instant lastUpdateFromServerTimestamp, Long latestEditSenderTimestampMs, Instant originalTimestamp, boolean wamoSub, NewsletterAdminProfile adminProfile, List<NewsletterPollVote> pollVotes) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.views = views;
        this.reactions = reactions;
        this.message = message;
        this.status = status;
        this.starred = starred;
        this.receipts = receipts;
        this.forwardsCount = forwardsCount;
        this.questionResponsesCount = questionResponsesCount;
        this.lastUpdateFromServerTimestamp = lastUpdateFromServerTimestamp;
        this.latestEditSenderTimestampMs = latestEditSenderTimestampMs;
        this.originalTimestamp = originalTimestamp;
        this.wamoSub = wamoSub;
        this.adminProfile = adminProfile;
        this.pollVotes = Objects.requireNonNullElseGet(pollVotes, ArrayList::new);
    }

    /**
     * Returns the message key that identifies this message.
     *
     * @return the message key, never {@code null}
     */
    @Override
    public MessageKey key() {
        return key;
    }

    /**
     * Sets the message key that identifies this message.
     *
     * @param key the new message key, must not be {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public void setKey(MessageKey key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Returns the server-assigned monotonic identifier.
     *
     * @return the server id
     */
    public int serverId() {
        return serverId;
    }

    /**
     * Sets the server-assigned monotonic identifier.
     *
     * @param serverId the new server id
     */
    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    /**
     * Returns the original send timestamp.
     *
     * @return an {@link Optional} holding the send timestamp, or empty if
     *         the server has not reported one
     */
    @Override
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the original send timestamp.
     *
     * @param timestamp the new send timestamp, or {@code null}
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the number of subscribers that have viewed this message.
     *
     * @return an {@link OptionalLong} holding the view count, or empty
     *         when no count has been reported yet
     */
    public OptionalLong views() {
        return views == null ? OptionalLong.empty() : OptionalLong.of(views);
    }

    /**
     * Sets the number of subscribers that have viewed this message.
     *
     * @param views the new view count, or {@code null}
     */
    public void setViews(Long views) {
        this.views = views;
    }

    /**
     * Replaces the aggregated reactions from a flat collection, collapsing
     * duplicate entries for the same emoji.
     *
     * <p>When two entries for the same emoji disagree on the count, the
     * merged entry takes the larger count; when they agree, the count is
     * incremented by one to reflect the combined tally. The local
     * {@code fromMe} flag is preserved from the first entry.
     *
     * @param reactions the reactions to aggregate, or {@code null} to
     *                  reset to an empty map
     */
    public void setReactions(Collection<NewsletterReaction> reactions) {
        if (reactions == null) {
            this.reactions = new HashMap<>();
        } else {
            this.reactions = reactions.stream().collect(Collectors.toMap(reaction -> reaction.content, Function.identity(), (firstReaction, secondReaction) -> {
                var firstReactionContent = firstReaction.content;
                var firstReactionCount = firstReaction.count;
                var firstReactionFromMe = firstReaction.fromMe;

                var secondReactionContent = secondReaction.content;
                var secondReactionCount = secondReaction.count;
                var secondReactionFromMe = secondReaction.fromMe;

                assert Objects.equals(firstReactionContent, secondReactionContent);
                assert firstReactionFromMe == secondReactionFromMe;

                if (firstReactionCount == secondReactionCount) {
                    return new NewsletterReaction(firstReactionContent, firstReactionCount + 1, firstReactionFromMe);
                } else {
                    return new NewsletterReaction(firstReactionContent, Math.max(firstReactionCount, secondReactionCount), firstReactionFromMe);
                }
            }));
        }
    }

    /**
     * Returns the container holding the message content.
     *
     * <p>If no content was delivered, an empty container is returned so
     * that callers can always chain getters without null checks.
     *
     * @return the message content container, never {@code null}
     */
    @Override
    public LinkedMessageContainer message() {
        return message != null ? message : LinkedMessageContainer.empty();
    }

    /**
     * Sets the container holding the message content.
     *
     * @param message the new content container, or {@code null}
     */
    public void setMessage(LinkedMessageContainer message) {
        this.message = message;
    }

    /**
     * Returns the delivery status of this message.
     *
     * @return an {@link Optional} holding the status, or empty when no
     *         status has been reported yet
     */
    @Override
    public Optional<MessageStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Sets the delivery status of this message.
     *
     * @param status the new status, or {@code null}
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * Replaces the delivery receipts associated with this message.
     *
     * @param receipts the new receipts, or {@code null}
     */
    public void setReceipts(List<MessageReceipt> receipts) {
        this.receipts = receipts;
    }

    /**
     * Returns the transient media handle produced by the upload pipeline.
     *
     * @return an {@link Optional} holding the handle, or empty when no
     *         upload has taken place
     */
    public Optional<String> mediaHandle() {
        return Optional.ofNullable(mediaHandle);
    }

    /**
     * Sets the transient media handle produced by the upload pipeline.
     *
     * @param mediaHandle the new handle, or {@code null}
     */
    public void setMediaHandle(String mediaHandle) {
        this.mediaHandle = mediaHandle;
    }

    /**
     * Returns an unmodifiable snapshot of the aggregated reactions.
     *
     * @return the reactions as an unmodifiable collection, never
     *         {@code null}
     */
    public Collection<NewsletterReaction> reactions() {
        return Collections.unmodifiableCollection(reactions.values());
    }

    /**
     * Looks up an aggregated reaction entry by its emoji content.
     *
     * @param value the emoji content to look up
     * @return an {@link Optional} holding the matching reaction, or empty
     *         when the emoji has no recorded tally
     */
    public Optional<NewsletterReaction> findReaction(String value) {
        return Optional.ofNullable(reactions.get(value));
    }

    /**
     * Inserts or replaces the tally entry for the supplied reaction.
     *
     * @param reaction the reaction whose tally to store
     * @return an {@link Optional} holding the previous tally for the same
     *         emoji, or empty when none existed
     */
    public Optional<NewsletterReaction> addReaction(NewsletterReaction reaction) {
        return Optional.ofNullable(reactions.put(reaction.content(), reaction));
    }

    /**
     * Removes the tally entry for the supplied emoji.
     *
     * @param code the emoji content whose tally to remove
     * @return an {@link Optional} holding the removed tally, or empty
     *         when no tally existed
     */
    public Optional<NewsletterReaction> removeReaction(String code) {
        return Optional.ofNullable(reactions.remove(code));
    }

    /**
     * Increments the tally for the supplied emoji, creating a new entry
     * with count {@code 1} when none exists.
     *
     * <p>This method also refreshes the local-participation flag so that
     * the latest action is attributed correctly.
     *
     * @param code   the emoji content whose tally to increment
     * @param fromMe {@code true} when the increment is caused by the
     *               local user
     */
    public void incrementReaction(String code, boolean fromMe) {
        findReaction(code).ifPresentOrElse(reaction -> {
            reaction.setCount(reaction.count() + 1);
            reaction.setFromMe(fromMe);
        }, () -> {
            var reaction = new NewsletterReaction(code, 1, fromMe);
            addReaction(reaction);
        });
    }

    /**
     * Decrements the tally for the supplied emoji, removing the entry
     * entirely when the count would drop to zero.
     *
     * <p>When the entry survives, its local-participation flag is reset
     * to {@code false} on the assumption that the local user just retracted
     * their reaction.
     *
     * @param code the emoji content whose tally to decrement
     */
    public void decrementReaction(String code) {
        findReaction(code).ifPresent(reaction -> {
            if (reaction.count() <= 1) {
                removeReaction(reaction.content());
                return;
            }

            reaction.setCount(reaction.count() - 1);
            reaction.setFromMe(false);
        });
    }

    /**
     * Returns whether the current viewer has starred this message.
     *
     * @return {@code true} when the message is starred
     */
    @Override
    public boolean starred() {
        return starred;
    }

    /**
     * Sets whether the current viewer has starred this message.
     *
     * @param starred the new starred flag
     */
    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    /**
     * Returns an unmodifiable view of the delivery receipts associated
     * with this message.
     *
     * @return the delivery receipts, never {@code null}
     */
    @Override
    public List<MessageReceipt> receipts() {
        return receipts == null ? List.of() : Collections.unmodifiableList(receipts);
    }

    /**
     * Returns the total number of times this message has been forwarded.
     *
     * @return an {@link OptionalLong} holding the forwards count, or
     *         empty when no count has been reported yet
     */
    public OptionalLong forwardsCount() {
        return forwardsCount == null ? OptionalLong.empty() : OptionalLong.of(forwardsCount);
    }

    /**
     * Sets the total number of times this message has been forwarded.
     *
     * @param forwardsCount the new forwards count, or {@code null}
     */
    public void setForwardsCount(Long forwardsCount) {
        this.forwardsCount = forwardsCount;
    }

    /**
     * Returns the number of responses received when this message is a
     * question post.
     *
     * @return an {@link OptionalLong} holding the response count, or
     *         empty when the message is not a question or no responses
     *         exist yet
     */
    public OptionalLong questionResponsesCount() {
        return questionResponsesCount == null ? OptionalLong.empty() : OptionalLong.of(questionResponsesCount);
    }

    /**
     * Sets the number of responses received when this message is a
     * question post.
     *
     * @param questionResponsesCount the new response count, or {@code null}
     */
    public void setQuestionResponsesCount(Long questionResponsesCount) {
        this.questionResponsesCount = questionResponsesCount;
    }

    /**
     * Returns the most recent moment at which the server reported
     * updated aggregates for this message.
     *
     * @return an {@link Optional} holding the last update timestamp, or
     *         empty when none has been reported
     */
    public Optional<Instant> lastUpdateFromServerTimestamp() {
        return Optional.ofNullable(lastUpdateFromServerTimestamp);
    }

    /**
     * Sets the last server-aggregate update timestamp.
     *
     * @param lastUpdateFromServerTimestamp the new timestamp, or
     *                                      {@code null}
     */
    public void setLastUpdateFromServerTimestamp(Instant lastUpdateFromServerTimestamp) {
        this.lastUpdateFromServerTimestamp = lastUpdateFromServerTimestamp;
    }

    /**
     * Returns the sender-side timestamp, in milliseconds, of the most
     * recent edit.
     *
     * @return an {@link OptionalLong} holding the edit timestamp, or
     *         empty when the message has never been edited
     */
    public OptionalLong latestEditSenderTimestampMs() {
        return latestEditSenderTimestampMs == null ? OptionalLong.empty() : OptionalLong.of(latestEditSenderTimestampMs);
    }

    /**
     * Sets the sender-side timestamp, in milliseconds, of the most recent
     * edit.
     *
     * @param latestEditSenderTimestampMs the new edit timestamp, or
     *                                    {@code null}
     */
    public void setLatestEditSenderTimestampMs(Long latestEditSenderTimestampMs) {
        this.latestEditSenderTimestampMs = latestEditSenderTimestampMs;
    }

    /**
     * Returns the original send timestamp before any edits were applied.
     *
     * @return an {@link Optional} holding the original timestamp, or
     *         empty when the message has never been edited or the server
     *         has not reported one
     */
    public Optional<Instant> originalTimestamp() {
        return Optional.ofNullable(originalTimestamp);
    }

    /**
     * Sets the original send timestamp before any edits were applied.
     *
     * @param originalTimestamp the new original timestamp, or {@code null}
     */
    public void setOriginalTimestamp(Instant originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    /**
     * Returns whether this message is gated behind a WAMO subscription.
     *
     * @return {@code true} when the message is WAMO-gated
     */
    public boolean wamoSub() {
        return wamoSub;
    }

    /**
     * Sets whether this message is gated behind a WAMO subscription.
     *
     * @param wamoSub the new WAMO-gated flag
     */
    public void setWamoSub(boolean wamoSub) {
        this.wamoSub = wamoSub;
    }

    /**
     * Returns the admin profile that published this message, when
     * admin-profile attribution is in use.
     *
     * @return an {@link Optional} holding the admin profile, or empty
     *         when the message is published under the newsletter itself
     */
    public Optional<NewsletterAdminProfile> adminProfile() {
        return Optional.ofNullable(adminProfile);
    }

    /**
     * Sets the admin profile that published this message.
     *
     * @param adminProfile the new admin profile, or {@code null}
     */
    public void setAdminProfile(NewsletterAdminProfile adminProfile) {
        this.adminProfile = adminProfile;
    }

    /**
     * Returns an unmodifiable view of the aggregated poll vote tallies
     * for this message.
     *
     * @return the poll vote tallies, never {@code null}
     */
    public List<NewsletterPollVote> pollVotes() {
        return Collections.unmodifiableList(pollVotes);
    }

    /**
     * Replaces the aggregated poll vote tallies for this message.
     *
     * @param pollVotes the new tallies, defaulted to an empty mutable
     *                  list when {@code null}
     */
    public void setPollVotes(List<NewsletterPollVote> pollVotes) {
        this.pollVotes = Objects.requireNonNullElseGet(pollVotes, ArrayList::new);
    }

    /**
     * Returns whether this message equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterMessageInfo}
     *         whose fields are all equal to this one's
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterMessageInfo that
               && serverId == that.serverId
               && starred == that.starred
               && wamoSub == that.wamoSub
               && Objects.equals(key, that.key)
               && Objects.equals(timestamp, that.timestamp)
               && Objects.equals(views, that.views)
               && Objects.equals(reactions, that.reactions)
               && Objects.equals(message, that.message)
               && status == that.status
               && Objects.equals(forwardsCount, that.forwardsCount)
               && Objects.equals(questionResponsesCount, that.questionResponsesCount)
               && Objects.equals(lastUpdateFromServerTimestamp, that.lastUpdateFromServerTimestamp)
               && Objects.equals(latestEditSenderTimestampMs, that.latestEditSenderTimestampMs)
               && Objects.equals(originalTimestamp, that.originalTimestamp)
               && Objects.equals(adminProfile, that.adminProfile)
               && Objects.equals(pollVotes, that.pollVotes);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this message
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, serverId, timestamp, views, reactions, message, status, starred, forwardsCount, questionResponsesCount, lastUpdateFromServerTimestamp, latestEditSenderTimestampMs, originalTimestamp, wamoSub, adminProfile, pollVotes);
    }
}
