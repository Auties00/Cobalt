package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a WhatsApp newsletter, also known as a channel.
 *
 * <p>A newsletter is a one-to-many broadcast surface where administrators publish
 * content that subscribers can read, react to, and forward. Unlike chats, newsletter
 * messages are not end-to-end encrypted: payloads travel as plaintext protobuf and
 * are decoded using the standard message specification.
 *
 * <p>This class is the top-level aggregate for a newsletter and bundles:
 * <ul>
 *   <li>its {@link Jid} identifier</li>
 *   <li>the {@link NewsletterState} describing whether the channel is active,
 *       suspended, or geo-suspended</li>
 *   <li>the {@link NewsletterMetadata} exposing name, description, pictures,
 *       handle, privacy, invite code, and other administrative fields</li>
 *   <li>the {@link NewsletterViewerMetadata} describing the current viewer's
 *       role and mute preference</li>
 *   <li>a count of unread messages and a last-activity timestamp</li>
 *   <li>an abstract message collection whose storage strategy is provided by
 *       the concrete subclass selected by the store implementation</li>
 * </ul>
 *
 * <p>Only the message-collection operations are abstract; metadata accessors
 * and mutators are final and share the same semantics across all subclasses.
 */
@ProtobufMessage
public abstract class Newsletter implements JidProvider {
    /**
     * The JID that uniquely identifies this newsletter on the WhatsApp network.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private Jid jid;

    /**
     * The current lifecycle state of this newsletter (for example active or
     * suspended). May be {@code null} when the state has not been reported by
     * the server.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private NewsletterState state;

    /**
     * The administrative metadata of this newsletter such as name,
     * description, picture, handle, and privacy. May be {@code null} when
     * metadata has not yet been fetched.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private NewsletterMetadata metadata;

    /**
     * The metadata describing the current viewer's relationship to this
     * newsletter, including their role and mute preference. May be
     * {@code null} when the current session has no relationship with the
     * newsletter.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private NewsletterViewerMetadata viewerMetadata;

    /**
     * The number of messages the current viewer has not yet read.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    private int unreadMessagesCount;

    /**
     * The timestamp of the most recent activity in this newsletter, used to
     * order newsletters in the channel list.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    private Instant timestamp;

    /**
     * Constructs a new {@code Newsletter} with the supplied identifier,
     * state, metadata, viewer metadata, unread counter, and activity
     * timestamp. Invoked by the generated protobuf deserializer and by
     * concrete subclasses.
     *
     * @param jid                 the newsletter JID
     * @param state               the current lifecycle state, or {@code null}
     * @param metadata            the administrative metadata, or {@code null}
     * @param viewerMetadata      the viewer relationship metadata, or {@code null}
     * @param unreadMessagesCount the number of unread messages
     * @param timestamp           the last-activity timestamp, or {@code null}
     */
    protected Newsletter(Jid jid, NewsletterState state, NewsletterMetadata metadata, NewsletterViewerMetadata viewerMetadata, int unreadMessagesCount, Instant timestamp) {
        this.jid = jid;
        this.state = state;
        this.metadata = metadata;
        this.viewerMetadata = viewerMetadata;
        this.unreadMessagesCount = unreadMessagesCount;
        this.timestamp = timestamp;
    }

    /**
     * Appends the given message to this newsletter's message collection.
     *
     * <p>The ordering policy (chronological, server-id order, or insertion
     * order) is defined by the concrete subclass.
     *
     * @param info the message to add
     */
    public abstract void addMessage(NewsletterMessageInfo info);

    /**
     * Removes a stored message by its client identifier.
     *
     * @param messageId the message identifier to remove
     * @return {@code true} if a message with the given identifier existed and
     *         was removed, {@code false} otherwise
     */
    public abstract boolean removeMessage(String messageId);

    /**
     * Clears every stored message in this newsletter.
     */
    public abstract void removeMessages();

    /**
     * Returns the stored messages as a {@link Stream} of
     * {@link NewsletterMessageInfo}, in server-id order.
     *
     * <p>The returned stream is {@link AutoCloseable}: implementations that
     * back the newsletter with an external resource keep that resource open
     * for the lifetime of the stream and release it when the stream is
     * closed. Callers MUST consume the stream inside a try-with-resources
     * block, otherwise the underlying resource is leaked.
     *
     * @return a non-null stream of newsletter messages, in server-id order
     */
    public abstract Stream<NewsletterMessageInfo> messages();

    /**
     * Returns the number of messages stored in this newsletter.
     *
     * <p>Provided as a dedicated accessor so callers do not have to consume
     * {@link #messages()} just to count. Both the in-memory and the
     * persistent backing implementations answer this in constant time.
     *
     * @return the number of messages currently stored in this newsletter
     */
    public abstract int messageCount();

    /**
     * Looks up a stored message by its client identifier.
     *
     * @param messageId the message identifier to look up
     * @return an {@link Optional} holding the matching message, or empty if
     *         no such message is stored
     */
    public abstract Optional<NewsletterMessageInfo> getMessageById(String messageId);

    /**
     * Returns the oldest stored message, typically the one with the lowest
     * server-assigned identifier.
     *
     * @return an {@link Optional} holding the oldest message, or empty if the
     *         collection is empty
     */
    public abstract Optional<NewsletterMessageInfo> oldestMessage();

    /**
     * Returns the most recently stored message, typically the one with the
     * highest server-assigned identifier.
     *
     * @return an {@link Optional} holding the newest message, or empty if the
     *         collection is empty
     */
    public abstract Optional<NewsletterMessageInfo> newestMessage();

    /**
     * Returns the JID that identifies this newsletter.
     *
     * @return the newsletter JID, never {@code null} for a well-formed instance
     */
    @Override
    public Jid toJid() {
        return jid;
    }

    /**
     * Returns the JID that identifies this newsletter.
     *
     * @return the newsletter JID, never {@code null} for a well-formed instance
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Sets the JID that identifies this newsletter.
     *
     * @param jid the newsletter JID, must not be {@code null}
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    public void setJid(Jid jid) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
    }

    /**
     * Returns the current lifecycle state of this newsletter.
     *
     * @return an {@link Optional} holding the state, or empty if the server
     *         has not reported one
     */
    public Optional<NewsletterState> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Sets the current lifecycle state of this newsletter.
     *
     * @param state the new state, or {@code null} to clear it
     */
    public void setState(NewsletterState state) {
        this.state = state;
    }

    /**
     * Returns the administrative metadata of this newsletter.
     *
     * @return an {@link Optional} holding the metadata, or empty if it has
     *         not been fetched
     */
    public Optional<NewsletterMetadata> metadata() {
        return Optional.ofNullable(metadata);
    }

    /**
     * Sets the administrative metadata of this newsletter.
     *
     * @param metadata the new metadata, or {@code null} to clear it
     */
    public void setMetadata(NewsletterMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the metadata describing the current viewer's relationship to
     * this newsletter.
     *
     * @return an {@link Optional} holding the viewer metadata, or empty if
     *         the current session has no relationship with the newsletter
     */
    public Optional<NewsletterViewerMetadata> viewerMetadata() {
        return Optional.ofNullable(viewerMetadata);
    }

    /**
     * Sets the viewer metadata for this newsletter.
     *
     * @param viewerMetadata the new viewer metadata, or {@code null} to clear it
     */
    public void setViewerMetadata(NewsletterViewerMetadata viewerMetadata) {
        this.viewerMetadata = viewerMetadata;
    }

    /**
     * Returns the number of messages the current viewer has not yet read.
     *
     * @return the unread message counter
     */
    public int unreadMessagesCount() {
        return unreadMessagesCount;
    }

    /**
     * Sets the number of messages the current viewer has not yet read.
     *
     * @param unreadMessagesCount the new unread counter
     */
    public void setUnreadMessagesCount(int unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
    }

    /**
     * Returns the timestamp of the most recent activity in this newsletter.
     *
     * @return an {@link Optional} holding the last-activity timestamp, or
     *         empty if not reported
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the timestamp of the most recent activity in this newsletter.
     *
     * @param timestamp the new last-activity timestamp, or {@code null} to
     *                  clear it
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns whether this {@code Newsletter} equals the supplied object.
     *
     * <p>Two newsletters are equal when their JID, state, metadata, viewer
     * metadata, unread counter, and timestamp are all equal. The message
     * collection is deliberately excluded so that subclasses can use
     * different storage strategies without affecting equality.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} represents the same newsletter
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof Newsletter that
                            && unreadMessagesCount == that.unreadMessagesCount
                            && Objects.equals(jid, that.jid)
                            && Objects.equals(state, that.state)
                            && Objects.equals(metadata, that.metadata)
                            && Objects.equals(viewerMetadata, that.viewerMetadata)
                            && Objects.equals(timestamp, that.timestamp);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this newsletter
     */
    @Override
    public int hashCode() {
        return Objects.hash(jid, state, metadata, viewerMetadata, unreadMessagesCount, timestamp);
    }
}
