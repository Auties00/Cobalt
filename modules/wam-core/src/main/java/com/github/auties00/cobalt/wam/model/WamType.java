package com.github.auties00.cobalt.wam.model;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;
import com.github.auties00.cobalt.wam.annotation.WamProperty;

/**
 * A wire-format type for WAM event fields, such as {@code INTEGER} or
 * {@code STRING}.
 *
 * <p>Each type controls how the annotation processor generates encoding
 * logic and constrains the permitted return type of the annotated method.
 * The mapping from {@code WamType} to Java type is:
 *
 * <ul>
 * <li>{@link #INTEGER} corresponds to {@code Long} (variable-width
 *     signed integer encoding: tag-only for 0 and 1, otherwise int8,
 *     int16, int32, or int64)
 * <li>{@link #BOOLEAN} corresponds to {@code Boolean} (encoded as the
 *     integer value {@code 0} or {@code 1})
 * <li>{@link #STRING} corresponds to {@code String} (length-prefixed
 *     UTF-8 with a uint8, uint16, or uint32 length header)
 * <li>{@link #FLOAT} corresponds to {@code Double} (IEEE 754
 *     double-precision, 8 bytes)
 * <li>{@link #TIMER} corresponds to {@code Instant} (identical wire
 *     encoding to {@code INTEGER}; the annotation processor additionally
 *     generates {@code startXxx()} and {@code stopXxx()} helper methods
 *     on the generated builder class for elapsed-time measurement)
 * <li>{@link #ENUM} corresponds to an {@code Optional} wrapping a
 *     {@link WamEnum}-annotated enum type (the constant's
 *     {@link WamEnumConstant#value()} is written as an integer)
 * </ul>
 *
 * @see WamProperty#type()
 */
public enum WamType {
    /**
     * A variable-width signed integer.
     *
     * <p>Values {@code 0} and {@code 1} are encoded in the tag byte alone
     * with no payload. Other values use the minimum number of bytes needed:
     * int8, int16, int32, or int64.
     *
     * <p>Java return type: {@code OptionalLong}.
     */
    INTEGER,

    /**
     * A boolean value encoded as the integer {@code 0} ({@code false}) or
     * {@code 1} ({@code true}).
     *
     * <p>Java return type: {@code Optional<Boolean>}.
     */
    BOOLEAN,

    /**
     * A length-prefixed UTF-8 string.
     *
     * <p>The length header is encoded as uint8 if the byte count is below
     * 256, uint16 if below 65536, or uint32 otherwise.
     *
     * <p>Java return type: {@code Optional<String>}.
     */
    STRING,

    /**
     * An IEEE 754 double-precision floating-point number (8 bytes).
     *
     * <p>Java return type: {@code OptionalDouble}.
     */
    FLOAT,

    /**
     * A timer field, encoded identically to {@link #INTEGER} on the wire.
     *
     * <p>The annotation processor generates {@code startXxx()} and
     * {@code stopXxx()} helper methods on the generated builder class
     * that compute elapsed milliseconds from a stored start timestamp.
     * Timer values are capped at {@code Integer.MAX_VALUE}.
     *
     * <p>Java return type: {@code Optional<Instant>}.
     */
    TIMER,

    /**
     * An enumeration property whose value is encoded as the constant's
     * integer index.
     *
     * <p>The Java return type must be {@code Optional} wrapping an enum
     * annotated with {@link WamEnum} whose constants are annotated with
     * {@link WamEnumConstant}. The annotation processor reads the
     * {@code WamEnumConstant} values at compile time and generates a
     * direct switch expression for encoding.
     */
    ENUM
}
