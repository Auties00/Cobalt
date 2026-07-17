package com.github.auties00.cobalt.calls.engine.control;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * An opaque per call privacy token the engine forwards uninterpreted.
 *
 * <p>A privacy token is a fixed size binary blob a caller attaches to a participant so the call plane can
 * present a privacy preserving identity for a contact whose phone number is private, without exposing the raw
 * phone number. The engine never reads inside the blob: it carries the token from the inbound offer or
 * membership stanza to the outbound action that addresses the participant again, byte for byte. This record
 * models that opaque payload as an immutable wrapper around the raw bytes, so the control layer can hold and
 * forward a token without ascribing any structure to it.
 *
 * <p>The canonical token is {@value #LENGTH} bytes. A token of any other length is still accepted and
 * forwarded, because the engine treats the blob as opaque and a future server build may resize it; the
 * {@link #isCanonicalLength()} predicate reports whether a token matches the size this build expects. The
 * wrapped array is defensively copied on the way in and out so the token is genuinely immutable.
 *
 * @implNote This implementation holds the token as an immutable byte wrapper rather than a parsed structure
 * because the blob's internal layout is opaque to the engine and is not part of the call protocol Cobalt
 * reproduces.
 */
public record PrivacyToken(byte[] value) {
    /**
     * The canonical length in bytes of a privacy token this build expects.
     *
     * <p>A token of a different length is still forwarded opaquely; only {@link #isCanonicalLength()}
     * distinguishes it from a token of this exact size.
     */
    public static final int LENGTH = 0x78;

    /**
     * Validates and defensively copies the token bytes.
     *
     * <p>The incoming array is cloned so later mutation of the caller's array cannot alter this token.
     *
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public PrivacyToken {
        Objects.requireNonNull(value, "value cannot be null");
        value = value.clone();
    }

    /**
     * Returns a copy of the raw token bytes.
     *
     * <p>The returned array is a fresh copy, so mutating it does not affect this token; this is the
     * canonical accessor a sender uses to stamp the opaque blob back onto an outbound stanza.
     *
     * @return a copy of the token bytes; never {@code null}
     */
    @Override
    public byte[] value() {
        return value.clone();
    }

    /**
     * Returns the length of this token in bytes.
     *
     * @return the token byte length
     */
    public int length() {
        return value.length;
    }

    /**
     * Returns whether this token matches the {@linkplain #LENGTH canonical length} this build expects.
     *
     * @return {@code true} when the token is exactly {@value #LENGTH} bytes
     */
    public boolean isCanonicalLength() {
        return value.length == LENGTH;
    }

    /**
     * Compares this token to another for value equality over its bytes.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link PrivacyToken} carrying the same bytes
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof PrivacyToken other && Arrays.equals(value, other.value));
    }

    /**
     * Returns a content based hash over the token bytes.
     *
     * @return the hash code of the token bytes
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    /**
     * Returns a diagnostic string that records the token length and bytes as hexadecimal.
     *
     * <p>The bytes are rendered as lowercase hexadecimal rather than as text because the blob is opaque
     * binary, not a string.
     *
     * @return a diagnostic representation of the token
     */
    @Override
    public String toString() {
        return "PrivacyToken[length=" + value.length + ", value=" + HexFormat.of().formatHex(value) + ']';
    }
}
