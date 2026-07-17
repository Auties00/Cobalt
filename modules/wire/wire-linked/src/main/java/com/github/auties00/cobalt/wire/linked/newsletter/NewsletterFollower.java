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
 * Represents a single follower entry returned by the newsletter follower
 * roster query.
 *
 * <p>Newsletter admins manage the follower roster from the channel
 * settings screen, where each row needs to display who the follower is,
 * what role they hold inside the channel, and when they joined. This
 * type bundles those three pieces of information together so that
 * callers can render an entry without having to thread several Optional
 * lookups together.
 *
 * <p>The {@link #jid()} accessor returns the follower's stable WhatsApp
 * identifier; the {@link #displayName()} accessor returns the
 * server-supplied push-name (when published) and the
 * {@link #username()} accessor returns the optional WhatsApp username,
 * also when the user has claimed one. The {@link #role()} accessor
 * exposes the channel-relative role used for permission checks, and
 * {@link #followTime()} carries the moment at which the follow happened
 * — useful both for sorting and for rendering "joined N days ago"
 * affordances.
 */
@ProtobufMessage
public final class NewsletterFollower {
    /**
     * The stable WhatsApp identifier of the follower.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The display name (push-name) currently published by the follower.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String displayName;

    /**
     * The phone number the follower has chosen to reveal alongside their
     * identity, when distinct from the JID user part.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String phoneNumber;

    /**
     * The WhatsApp username claimed by the follower, when one has been
     * published.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String username;

    /**
     * The channel-relative role assigned to the follower (typically
     * {@link NewsletterViewerRole#SUBSCRIBER}, {@link NewsletterViewerRole#ADMIN}
     * or {@link NewsletterViewerRole#OWNER}).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    NewsletterViewerRole role;

    /**
     * The instant at which this follower started following the newsletter.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant followTime;

    /**
     * Constructs a new {@code NewsletterFollower}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param jid          the stable WhatsApp identifier; must not be {@code null}
     * @param displayName  the push-name, may be {@code null}
     * @param phoneNumber  the disclosed phone number, may be {@code null}
     * @param username     the WhatsApp username, may be {@code null}
     * @param role         the channel-relative role, may be {@code null} when not reported
     * @param followTime   the follow instant, may be {@code null} when not reported
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    NewsletterFollower(Jid jid, String displayName, String phoneNumber, String username, NewsletterViewerRole role, Instant followTime) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.role = role;
        this.followTime = followTime;
    }

    /**
     * Returns the stable WhatsApp identifier of this follower.
     *
     * @return the follower JID, never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the display name (push-name) currently published by this
     * follower.
     *
     * @return an {@link Optional} carrying the display name, or empty when
     *         the follower has not published one
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the phone number this follower has chosen to disclose
     * alongside their identity.
     *
     * @return an {@link Optional} carrying the disclosed phone number, or
     *         empty when none is available
     */
    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the WhatsApp username claimed by this follower.
     *
     * @return an {@link Optional} carrying the username, or empty when no
     *         username is associated with this follower
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Returns the channel-relative role assigned to this follower.
     *
     * @return an {@link Optional} carrying the role, or empty when the
     *         server has not reported one
     */
    public Optional<NewsletterViewerRole> role() {
        return Optional.ofNullable(role);
    }

    /**
     * Returns the instant at which this follower started following the
     * newsletter.
     *
     * @return an {@link Optional} carrying the follow instant, or empty
     *         when the server has not reported one
     */
    public Optional<Instant> followTime() {
        return Optional.ofNullable(followTime);
    }

    /**
     * Returns whether this follower entry equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterFollower}
     *         carrying equal field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterFollower that
                && Objects.equals(jid, that.jid)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(phoneNumber, that.phoneNumber)
                && Objects.equals(username, that.username)
                && role == that.role
                && Objects.equals(followTime, that.followTime);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this follower entry
     */
    @Override
    public int hashCode() {
        return Objects.hash(jid, displayName, phoneNumber, username, role, followTime);
    }

    /**
     * Returns a debug-oriented string representation of this follower
     * entry.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterFollower[" +
                "jid=" + jid +
                ", displayName=" + displayName +
                ", phoneNumber=" + phoneNumber +
                ", username=" + username +
                ", role=" + role +
                ", followTime=" + followTime +
                ']';
    }
}
