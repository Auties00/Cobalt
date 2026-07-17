package com.github.auties00.cobalt.wire.core.mixin;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.Instant;

/**
 * Protobuf mixin that bridges the gap between {@link Instant} and millisecond-precision epoch
 * timestamps transmitted on the wire.
 *
 * <p>Protobuf messages cannot encode {@link Instant} values natively, so timestamp fields are
 * serialized as a {@code Long} counting milliseconds elapsed since {@code 1970-01-01T00:00:00Z}.
 * This mixin is installed automatically whenever a protobuf field typed as {@link Instant}
 * declares millisecond precision, allowing field accessors to expose the richer
 * {@link Instant} type while the serializer continues to read and write a plain {@code Long}.
 *
 * <p>Use {@link InstantSecondsMixin} instead for fields whose wire format uses seconds, which
 * is the more common convention across the WhatsApp protocol.
 */
@ProtobufMixin
public final class InstantMillisMixin {
    /**
     * Serializes an {@link Instant} into its millisecond epoch representation for transmission
     * on the wire.
     *
     * <p>The returned value counts milliseconds elapsed since {@code 1970-01-01T00:00:00Z} and
     * is {@code null}-safe so that optional timestamp fields can be left unset without forcing
     * a sentinel value.
     *
     * @param instant the instant to serialize, or {@code null} if the field is unset
     * @return the number of milliseconds since the epoch, or {@code null} when
     *         {@code instant} is {@code null}
     */
    @ProtobufSerializer
    public static Long toMillis(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    /**
     * Deserializes a millisecond epoch {@code Long} into an {@link Instant}.
     *
     * <p>The input is interpreted as milliseconds elapsed since {@code 1970-01-01T00:00:00Z}.
     * A {@code null} input yields a {@code null} instant so that optional protobuf fields can
     * remain absent after decoding.
     *
     * @param value the millisecond epoch value read from the wire, or {@code null} when the
     *              field was absent
     * @return the reconstructed {@link Instant}, or {@code null} when {@code value} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static Instant fromMillis(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }
}
