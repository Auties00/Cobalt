package com.github.auties00.cobalt.wire.linked.contact;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the full WhatsApp username record claimed by a single user.
 *
 * <p>WhatsApp lets users claim a global username distinct from their
 * phone-number identifier. Beyond the username text itself, the relay
 * reports the registration {@link #state()} of the claim, the
 * {@link #timestamp()} it was registered at, the recovery {@link #pin()}
 * bound to it, and a per-user fetch {@link #status()} describing whether
 * the lookup succeeded.
 *
 * <p>This carrier is a plain immutable value object: it surfaces the
 * parsed reply to caller code and never travels on the wire.
 */
public final class Username {
    /**
     * The claimed username identifier.
     */
    private final String username;

    /**
     * The registration state token of the username claim.
     */
    private final String state;

    /**
     * The instant the username was registered at.
     */
    private final Instant timestamp;

    /**
     * The recovery PIN hash bound to the username.
     */
    private final String pin;

    /**
     * The per-user fetch outcome reported by the relay.
     */
    private final String status;

    /**
     * Constructs a new username record.
     *
     * @param username  the claimed username identifier, may be
     *                  {@code null}
     * @param state     the registration state token, may be {@code null}
     * @param timestamp the registration instant, may be {@code null}
     * @param pin       the recovery PIN hash, may be {@code null}
     * @param status    the per-user fetch outcome, may be {@code null}
     */
    public Username(String username, String state, Instant timestamp, String pin, String status) {
        this.username = username;
        this.state = state;
        this.timestamp = timestamp;
        this.pin = pin;
        this.status = status;
    }

    /**
     * Returns the claimed username identifier.
     *
     * @return an {@link Optional} carrying the username, or empty when
     *         the user has not claimed one
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Returns the registration state token of the username claim.
     *
     * @return an {@link Optional} carrying the state token, or empty when
     *         the relay omitted it
     */
    public Optional<String> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Returns the instant the username was registered at.
     *
     * @return an {@link Optional} carrying the instant, or empty when the
     *         relay omitted it
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns the recovery PIN hash bound to the username.
     *
     * @return an {@link Optional} carrying the PIN hash, or empty when the
     *         relay omitted it
     */
    public Optional<String> pin() {
        return Optional.ofNullable(pin);
    }

    /**
     * Returns the per-user fetch outcome reported by the relay.
     *
     * @return an {@link Optional} carrying the status token, or empty when
     *         the relay omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns whether this record equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code UserUsername} carrying
     *         equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof Username that
                && Objects.equals(username, that.username)
                && Objects.equals(state, that.state)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(pin, that.pin)
                && Objects.equals(status, that.status);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(username, state, timestamp, pin, status);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "UserUsername[username=" + username +
                ", state=" + state +
                ", timestamp=" + timestamp +
                ", pin=" + pin +
                ", status=" + status + ']';
    }
}
