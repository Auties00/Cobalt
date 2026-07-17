package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Holds the success result of the disappearing-mode USync parser.
 *
 * Surfaced by USync queries that request the disappearing-mode protocol, such
 * as the interactive fetch for the disappearing-message picker, the background
 * contact sync, and the existence probe. Carries the peer's current
 * disappearing-message timer, the timestamp the setting was last changed at,
 * and the {@code ephemerality_disabled} flag for PA-thread support.
 *
 * @implNote
 * This implementation always materialises {@link #ephemeralityDisabled()}
 * regardless of WA Web's privacy gate, which only controls whether the field
 * is propagated to the JS UI; callers that want gating behaviour ignore the
 * value when the gate is off.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncDisappearingMode")
public final class DisappearingModeResult implements UsyncProtocolResponse {
    /**
     * Holds the disappearing-message timer, where {@link Duration#ZERO} means
     * the peer has the feature off.
     */
    private final Duration duration;

    /**
     * Holds the wall-clock instant the setting was last changed at, decoded
     * from the {@code t} attribute.
     */
    private final Instant timestamp;

    /**
     * Indicates whether ephemerality is disabled on the peer's PA
     * (personal-assistant) thread, decoded from
     * {@code ephemerality_disabled="true"}.
     */
    private final boolean ephemeralityDisabled;

    /**
     * Creates a new disappearing-mode result.
     *
     * @param duration             the timer; must not be {@code null}
     * @param timestamp            the last-changed timestamp; must not be
     *                             {@code null}
     * @param ephemeralityDisabled the {@code ephemerality_disabled} flag
     */
    public DisappearingModeResult(Duration duration, Instant timestamp, boolean ephemeralityDisabled) {
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.ephemeralityDisabled = ephemeralityDisabled;
    }

    /**
     * Returns the disappearing-message timer.
     *
     * {@link Duration#ZERO} means the peer disabled disappearing messages.
     *
     * @return the timer, never {@code null}
     */
    public Duration duration() {
        return duration;
    }

    /**
     * Returns the timestamp the setting was last changed at.
     *
     * Used by the picker UI to compare against the local "last edited"
     * timestamp when deciding whose change wins on conflict.
     *
     * @return the timestamp, never {@code null}
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns whether ephemerality is disabled on the peer's PA thread.
     *
     * PA (personal-assistant) threads do not honour the standard
     * disappearing-message timer when this flag is true; the UI hides the
     * timer chip in that case.
     *
     * @return {@code true} when {@code ephemerality_disabled="true"}
     */
    public boolean ephemeralityDisabled() {
        return ephemeralityDisabled;
    }
}
