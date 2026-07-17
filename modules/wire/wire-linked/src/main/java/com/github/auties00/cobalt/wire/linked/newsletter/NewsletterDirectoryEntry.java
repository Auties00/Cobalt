package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a single entry returned by the newsletter directory queries.
 *
 * <p>The newsletter directory is the explore surface where users browse
 * curated channels by category, country, popularity or recommendation.
 * Each result is a thin description of the channel that the directory
 * UI renders as a card: name, description, profile picture, handle,
 * subscriber count, verification badge and creation date. Cobalt
 * exposes that descriptor through this type rather than reflecting the
 * verbose GraphQL {@code thread_metadata} envelope, which contains
 * many transport-only fields the caller never inspects.
 *
 * <p>The {@link #jid()} accessor returns the channel JID, derived from
 * the GraphQL {@code id} scalar. The {@link #invite()} accessor returns
 * the public invite code that can be appended to
 * {@code chat.whatsapp.com/channel/} to deep-link to the channel.
 */
@ProtobufMessage
public final class NewsletterDirectoryEntry {
    /**
     * The channel JID.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The display name of the channel.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The textual description shown on the channel profile screen.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String description;

    /**
     * The unique handle deep-linking to this channel.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String handle;

    /**
     * The opaque invite code suffix used to share this channel.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String invite;

    /**
     * The verification state of this channel, as reported by the relay.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    NewsletterVerification verification;

    /**
     * The number of subscribers, when reported.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
    Long subscribersCount;

    /**
     * The instant at which this channel was created.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant creationTime;

    /**
     * The identifier of the profile picture handle on the media server.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String pictureId;

    /**
     * The direct path of the profile picture handle on the media server.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String pictureDirectPath;

    /**
     * Constructs a new {@code NewsletterDirectoryEntry}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param jid                the channel JID; must not be {@code null}
     * @param name               the display name, may be {@code null}
     * @param description        the description, may be {@code null}
     * @param handle             the handle, may be {@code null}
     * @param invite             the invite code, may be {@code null}
     * @param verification       the verification state, may be {@code null}
     * @param subscribersCount   the subscriber count, may be {@code null}
     * @param creationTime       the creation instant, may be {@code null}
     * @param pictureId          the profile picture identifier, may be {@code null}
     * @param pictureDirectPath  the profile picture direct path, may be {@code null}
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    NewsletterDirectoryEntry(Jid jid, String name, String description, String handle, String invite, NewsletterVerification verification, Long subscribersCount, Instant creationTime, String pictureId, String pictureDirectPath) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.name = name;
        this.description = description;
        this.handle = handle;
        this.invite = invite;
        this.verification = verification;
        this.subscribersCount = subscribersCount;
        this.creationTime = creationTime;
        this.pictureId = pictureId;
        this.pictureDirectPath = pictureDirectPath;
    }

    /**
     * Returns the channel JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the display name.
     *
     * @return an {@link Optional} carrying the display name, or empty when
     *         not reported
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the textual description.
     *
     * @return an {@link Optional} carrying the description, or empty when
     *         not reported
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the unique handle deep-linking to this channel.
     *
     * @return an {@link Optional} carrying the handle, or empty when not
     *         reported
     */
    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    /**
     * Returns the opaque invite code suffix.
     *
     * @return an {@link Optional} carrying the invite code, or empty when
     *         not reported
     */
    public Optional<String> invite() {
        return Optional.ofNullable(invite);
    }

    /**
     * Returns the verification state.
     *
     * @return an {@link Optional} carrying the verification state, or
     *         empty when not reported
     */
    public Optional<NewsletterVerification> verification() {
        return Optional.ofNullable(verification);
    }

    /**
     * Returns the number of subscribers.
     *
     * @return an {@link OptionalLong} carrying the subscriber count, or
     *         empty when not reported
     */
    public OptionalLong subscribersCount() {
        return subscribersCount == null ? OptionalLong.empty() : OptionalLong.of(subscribersCount);
    }

    /**
     * Returns the instant at which this channel was created.
     *
     * @return an {@link Optional} carrying the creation instant, or empty
     *         when not reported
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime);
    }

    /**
     * Returns the identifier of the profile picture handle on the media
     * server.
     *
     * @return an {@link Optional} carrying the picture identifier, or
     *         empty when not reported
     */
    public Optional<String> pictureId() {
        return Optional.ofNullable(pictureId);
    }

    /**
     * Returns the direct path of the profile picture handle on the media
     * server.
     *
     * @return an {@link Optional} carrying the direct path, or empty when
     *         not reported
     */
    public Optional<String> pictureDirectPath() {
        return Optional.ofNullable(pictureDirectPath);
    }

    /**
     * Returns whether this entry equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterDirectoryEntry} carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterDirectoryEntry that
                && Objects.equals(jid, that.jid)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(handle, that.handle)
                && Objects.equals(invite, that.invite)
                && Objects.equals(verification, that.verification)
                && Objects.equals(subscribersCount, that.subscribersCount)
                && Objects.equals(creationTime, that.creationTime)
                && Objects.equals(pictureId, that.pictureId)
                && Objects.equals(pictureDirectPath, that.pictureDirectPath);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(jid, name, description, handle, invite, verification, subscribersCount, creationTime, pictureId, pictureDirectPath);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterDirectoryEntry[" +
                "jid=" + jid +
                ", name=" + name +
                ", handle=" + handle +
                ", verification=" + verification +
                ", subscribersCount=" + subscribersCount +
                ']';
    }
}
