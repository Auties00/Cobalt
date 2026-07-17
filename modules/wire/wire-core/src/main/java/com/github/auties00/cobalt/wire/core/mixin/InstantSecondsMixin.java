package com.github.auties00.cobalt.wire.core.mixin;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.Instant;

/**
 * Protobuf mixin that bridges the gap between {@link Instant} and second-precision epoch
 * timestamps transmitted on the wire.
 *
 * <p>Most timestamp fields in the WhatsApp protocol are encoded as an integer number of
 * seconds elapsed since {@code 1970-01-01T00:00:00Z}. This mixin lets such fields be
 * declared as {@link Instant} in Cobalt's model classes while the serializer continues to
 * read and write the underlying numeric value. Both 32-bit and 64-bit encodings are
 * supported: {@code Long} is used when the schema declares a 64-bit integer, while
 * {@code Integer} is used for 32-bit integer fields.
 *
 * <p>Use {@link InstantMillisMixin} for fields whose wire format uses milliseconds rather
 * than seconds.
 */
@ProtobufMixin
public final class InstantSecondsMixin {
    /**
     * Serializes an {@link Instant} into a 64-bit second epoch representation for
     * transmission on the wire.
     *
     * <p>The returned value counts seconds elapsed since {@code 1970-01-01T00:00:00Z} and is
     * {@code null}-safe so that optional timestamp fields can remain unset. Sub-second
     * components of the instant are discarded.
     *
     * @param instant the instant to serialize, or {@code null} if the field is unset
     * @return the number of seconds since the epoch, or {@code null} when {@code instant}
     *         is {@code null}
     */
    @ProtobufSerializer
    public static Long toSecondsLong(Instant instant) {
        return instant == null ? null : instant.getEpochSecond();
    }

    /**
     * Deserializes a 64-bit second epoch {@code Long} into an {@link Instant}.
     *
     * <p>The input is interpreted as seconds elapsed since {@code 1970-01-01T00:00:00Z}.
     * A {@code null} input yields a {@code null} instant so that optional protobuf fields
     * can remain absent after decoding.
     *
     * @param value the second epoch value read from the wire, or {@code null} when the
     *              field was absent
     * @return the reconstructed {@link Instant}, or {@code null} when {@code value} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static Instant fromSecondsLong(Long value) {
        return value == null ? null : Instant.ofEpochSecond(value);
    }

    /**
     * Serializes an {@link Instant} into a 32-bit second epoch representation for
     * transmission on the wire.
     *
     * <p>This overload is used when the protobuf schema declares the timestamp field as a
     * 32-bit integer. The second count is narrowed from {@code long} to {@code int};
     * timestamps beyond the range of {@link Integer#MAX_VALUE} (roughly the year 2038) will
     * overflow silently, so this variant should only be chosen when the schema explicitly
     * requires it.
     *
     * @param instant the instant to serialize, or {@code null} if the field is unset
     * @return the number of seconds since the epoch truncated to a 32-bit integer, or
     *         {@code null} when {@code instant} is {@code null}
     */
    @ProtobufSerializer
    public static Integer toSecondsInt(Instant instant) {
        return instant == null ? null : (int) instant.getEpochSecond();
    }

    /**
     * Deserializes a 32-bit second epoch {@code Integer} into an {@link Instant}.
     *
     * <p>The input is interpreted as seconds elapsed since {@code 1970-01-01T00:00:00Z}.
     * A {@code null} input yields a {@code null} instant so that optional protobuf fields
     * can remain absent after decoding.
     *
     * @param value the 32-bit second epoch value read from the wire, or {@code null} when
     *              the field was absent
     * @return the reconstructed {@link Instant}, or {@code null} when {@code value} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static Instant fromSecondsInt(Integer value) {
        return value == null ? null : Instant.ofEpochSecond(value);
    }
}
