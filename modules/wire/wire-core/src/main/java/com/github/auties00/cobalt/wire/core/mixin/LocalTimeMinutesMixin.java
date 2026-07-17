package com.github.auties00.cobalt.wire.core.mixin;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.LocalTime;

/**
 * Protobuf mixin that bridges the gap between {@link LocalTime} and the minute-of-day
 * integer used on the wire for time-only fields.
 *
 * <p>The WhatsApp protocol expresses certain time-of-day values (such as business hour
 * schedules and do-not-disturb windows) as an integer counting minutes from midnight, where
 * {@code 0} is {@code 00:00} and {@code 1439} is {@code 23:59}. This mixin lets these
 * fields be declared as {@link LocalTime} in Cobalt's model classes while the serializer
 * continues to read and write the underlying integer. Both 32-bit and 64-bit encodings are
 * supported: {@code Long} is used when the schema declares a 64-bit integer, while
 * {@code Integer} is used for 32-bit integer fields.
 */
@ProtobufMixin
public final class LocalTimeMinutesMixin {
    /**
     * Serializes a {@link LocalTime} into a 64-bit minute-of-day representation for
     * transmission on the wire.
     *
     * <p>The returned value counts minutes elapsed since midnight. Seconds and nanoseconds
     * in the input are truncated, so {@code 09:30:45} and {@code 09:30:00} both serialize
     * to {@code 570}.
     *
     * @param time the local time to serialize, or {@code null} if the field is unset
     * @return the number of minutes from midnight, or {@code null} when {@code time} is
     *         {@code null}
     */
    @ProtobufSerializer
    public static Long toMinutesLong(LocalTime time) {
        return time == null ? null : time.toSecondOfDay() / 60L;
    }

    /**
     * Deserializes a 64-bit minute-of-day {@code Long} into a {@link LocalTime}.
     *
     * <p>The input is interpreted as minutes elapsed since midnight. A {@code null} input
     * yields a {@code null} time so that optional protobuf fields can remain absent after
     * decoding.
     *
     * @param value the minute-of-day value read from the wire, or {@code null} when the
     *              field was absent
     * @return the reconstructed {@link LocalTime}, or {@code null} when {@code value} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static LocalTime fromMinutesLong(Long value) {
        return value == null ? null : LocalTime.ofSecondOfDay(value * 60);
    }

    /**
     * Serializes a {@link LocalTime} into a 32-bit minute-of-day representation for
     * transmission on the wire.
     *
     * <p>This overload is used when the protobuf schema declares the field as a 32-bit
     * integer. Seconds and nanoseconds in the input are truncated.
     *
     * @param time the local time to serialize, or {@code null} if the field is unset
     * @return the number of minutes from midnight, or {@code null} when {@code time} is
     *         {@code null}
     */
    @ProtobufSerializer
    public static Integer toMinutesInt(LocalTime time) {
        return time == null ? null : time.toSecondOfDay() / 60;
    }

    /**
     * Deserializes a 32-bit minute-of-day {@code Integer} into a {@link LocalTime}.
     *
     * <p>The input is interpreted as minutes elapsed since midnight. A {@code null} input
     * yields a {@code null} time so that optional protobuf fields can remain absent after
     * decoding.
     *
     * @param value the 32-bit minute-of-day value read from the wire, or {@code null} when
     *              the field was absent
     * @return the reconstructed {@link LocalTime}, or {@code null} when {@code value} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static LocalTime fromMinutesInt(Integer value) {
        return value == null ? null : LocalTime.ofSecondOfDay(value * 60L);
    }
}
