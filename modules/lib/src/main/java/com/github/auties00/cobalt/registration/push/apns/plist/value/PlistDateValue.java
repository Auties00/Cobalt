package com.github.auties00.cobalt.registration.push.apns.plist.value;

import java.time.Instant;
import java.util.Objects;

/**
 * Holds a plist {@code <date>} leaf, the binary plist {@code 0x33} marker.
 *
 * <p>An instance represents a wall-clock timestamp carried by an APNS plist payload. The binary
 * representation is a big-endian {@code float64} of seconds since the Apple reference date
 * (2001-01-01 UTC); it is rebased onto the Unix epoch and surfaced as a UTC {@link Instant} so
 * consumers always work with a standard value.
 *
 * @implNote This implementation stores the converted {@link Instant} directly rather than the raw
 *           Apple-epoch double; the epoch offset is applied once at parse time.
 * @param value the timestamp
 */
public record PlistDateValue(Instant value) implements PlistValue {
    /**
     * Constructs a date leaf, rejecting a {@code null} instant.
     *
     * @param value the timestamp
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public PlistDateValue {
        Objects.requireNonNull(value, "value");
    }
}
