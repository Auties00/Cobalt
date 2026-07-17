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
 * Represents a pending administrator invitation attached to a newsletter.
 *
 * <p>A newsletter owner may invite other users to become co-administrators
 * of the channel. Until the invitee accepts, the invitation lives on the
 * server as a pending entry that surfaces in two places: on the owner UI
 * (so they can revoke a mistakenly issued invitation) and on the
 * invitee's own UI (so they can accept it). Both surfaces share the
 * same shape, which this class captures.
 *
 * <p>The {@link #invitee()} accessor returns the JID of the user the
 * invitation is addressed to, while the {@link #expirationTime()}
 * accessor returns the moment past which the invitation can no longer be
 * accepted. Server policies — notably the maximum number of pending
 * invitations per channel — are enforced on the relay; this type is
 * a passive descriptor and does not validate them.
 */
@ProtobufMessage
public final class NewsletterAdminInvite {
    /**
     * The JID of the user the invitation is addressed to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid invitee;

    /**
     * The phone number disclosed alongside the invitee identity, when
     * available.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String inviteePhoneNumber;

    /**
     * The instant past which the invitation can no longer be accepted.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant expirationTime;

    /**
     * Constructs a new {@code NewsletterAdminInvite}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param invitee            the JID of the invitee; must not be {@code null}
     * @param inviteePhoneNumber the disclosed phone number, may be {@code null}
     * @param expirationTime     the expiration instant, may be {@code null} when
     *                           the relay has not reported one
     * @throws NullPointerException if {@code invitee} is {@code null}
     */
    NewsletterAdminInvite(Jid invitee, String inviteePhoneNumber, Instant expirationTime) {
        this.invitee = Objects.requireNonNull(invitee, "invitee cannot be null");
        this.inviteePhoneNumber = inviteePhoneNumber;
        this.expirationTime = expirationTime;
    }

    /**
     * Returns the JID of the user this invitation is addressed to.
     *
     * @return the invitee JID, never {@code null}
     */
    public Jid invitee() {
        return invitee;
    }

    /**
     * Returns the phone number the invitee has disclosed alongside their
     * identity, when available.
     *
     * @return an {@link Optional} carrying the phone number, or empty when
     *         none is available
     */
    public Optional<String> inviteePhoneNumber() {
        return Optional.ofNullable(inviteePhoneNumber);
    }

    /**
     * Returns the instant past which this invitation can no longer be
     * accepted.
     *
     * @return an {@link Optional} carrying the expiration instant, or
     *         empty when the relay has not reported one
     */
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    /**
     * Returns whether this invitation equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterAdminInvite}
     *         carrying equal field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterAdminInvite that
                && Objects.equals(invitee, that.invitee)
                && Objects.equals(inviteePhoneNumber, that.inviteePhoneNumber)
                && Objects.equals(expirationTime, that.expirationTime);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this invitation
     */
    @Override
    public int hashCode() {
        return Objects.hash(invitee, inviteePhoneNumber, expirationTime);
    }

    /**
     * Returns a debug-oriented string representation of this invitation.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterAdminInvite[" +
                "invitee=" + invitee +
                ", inviteePhoneNumber=" + inviteePhoneNumber +
                ", expirationTime=" + expirationTime +
                ']';
    }
}
