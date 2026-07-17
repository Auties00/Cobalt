package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single response to a newsletter question post.
 *
 * <p>Newsletter authors can publish "question" posts asking their
 * subscribers a free-form question; subscribers reply with a
 * free-form text answer. The author then sees the aggregated list of
 * replies in a dedicated panel, with one row per response carrying
 * the responder's anonymised identity, their picture, the response
 * timestamp, and a flag indicating whether the author has already
 * replied to that response.
 *
 * <p>The {@link #responderLid()} accessor returns the responder's
 * Lightweight Identifier (a relay-managed pseudonymous identity
 * specific to the newsletter context, distinct from the responder's
 * real JID) when available; the {@link #fromSelf()} accessor returns
 * {@code true} when the response was authored by the connected
 * account, in which case the responder identity is the local user
 * itself rather than another subscriber.
 */
@ProtobufMessage
public final class NewsletterQuestionResponse {
    /**
     * The stanza id of the response message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String messageId;

    /**
     * The moment at which the response was published.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Whether the response was authored by the connected account.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean fromSelf;

    /**
     * The responder's Lightweight Identifier within the newsletter
     * context, when supplied by the relay.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    Jid responderLid;

    /**
     * The responder's display name as published in the newsletter
     * context, when supplied by the relay.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String responderDisplayName;

    /**
     * The direct path of the responder's profile picture on the
     * media server.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String responderProfilePictureDirectPath;

    /**
     * Whether the author of the question has already replied to this
     * response.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean repliedByAuthor;

    /**
     * Constructs a new {@code NewsletterQuestionResponse}. Invoked by
     * the generated protobuf deserializer and by the converters that
     * adapt wire responses into the domain model.
     *
     * @param messageId                         the stanza id of the
     *                                          response; must not be
     *                                          {@code null}
     * @param timestamp                         the publish time; may
     *                                          be {@code null} when
     *                                          unreported
     * @param fromSelf                          whether the response
     *                                          was authored by the
     *                                          connected account
     * @param responderLid                      the optional
     *                                          responder LID; may be
     *                                          {@code null}
     * @param responderDisplayName              the optional display
     *                                          name; may be
     *                                          {@code null}
     * @param responderProfilePictureDirectPath the optional profile
     *                                          picture direct path;
     *                                          may be {@code null}
     * @param repliedByAuthor                   whether the question
     *                                          author has already
     *                                          replied to this
     *                                          response
     * @throws NullPointerException if {@code messageId} is
     *                              {@code null}
     */
    NewsletterQuestionResponse(String messageId,
                               Instant timestamp,
                               boolean fromSelf,
                               Jid responderLid,
                               String responderDisplayName,
                               String responderProfilePictureDirectPath,
                               boolean repliedByAuthor) {
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        this.timestamp = timestamp;
        this.fromSelf = fromSelf;
        this.responderLid = responderLid;
        this.responderDisplayName = responderDisplayName;
        this.responderProfilePictureDirectPath = responderProfilePictureDirectPath;
        this.repliedByAuthor = repliedByAuthor;
    }

    /**
     * Returns the stanza id of the response message.
     *
     * @return the message id, never {@code null}
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the moment at which the response was published.
     *
     * @return an {@link Optional} carrying the publish time, or
     *         empty when the relay omitted it
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns whether the response was authored by the connected
     * account.
     *
     * @return {@code true} when the response originated from this
     *         account
     */
    public boolean fromSelf() {
        return fromSelf;
    }

    /**
     * Returns the responder's Lightweight Identifier within the
     * newsletter context.
     *
     * @return an {@link Optional} carrying the LID, or empty when
     *         the relay omitted it
     */
    public Optional<Jid> responderLid() {
        return Optional.ofNullable(responderLid);
    }

    /**
     * Returns the responder's display name as published in the
     * newsletter context.
     *
     * @return an {@link Optional} carrying the display name, or
     *         empty when the relay omitted it
     */
    public Optional<String> responderDisplayName() {
        return Optional.ofNullable(responderDisplayName);
    }

    /**
     * Returns the direct path of the responder's profile picture on
     * the media server.
     *
     * @return an {@link Optional} carrying the direct path, or empty
     *         when the relay omitted it
     */
    public Optional<String> responderProfilePictureDirectPath() {
        return Optional.ofNullable(responderProfilePictureDirectPath);
    }

    /**
     * Returns whether the author of the question has already replied
     * to this response.
     *
     * @return {@code true} when an author reply has been recorded
     */
    public boolean repliedByAuthor() {
        return repliedByAuthor;
    }

    /**
     * Returns whether this response equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterQuestionResponse} carrying equal
     *         field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterQuestionResponse that
                && fromSelf == that.fromSelf
                && repliedByAuthor == that.repliedByAuthor
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(responderLid, that.responderLid)
                && Objects.equals(responderDisplayName, that.responderDisplayName)
                && Objects.equals(responderProfilePictureDirectPath, that.responderProfilePictureDirectPath);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(messageId, timestamp, fromSelf, responderLid,
                responderDisplayName, responderProfilePictureDirectPath, repliedByAuthor);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterQuestionResponse[" +
                "messageId=" + messageId +
                ", timestamp=" + timestamp +
                ", fromSelf=" + fromSelf +
                ", responderLid=" + responderLid +
                ", responderDisplayName=" + responderDisplayName +
                ", repliedByAuthor=" + repliedByAuthor +
                ']';
    }
}
