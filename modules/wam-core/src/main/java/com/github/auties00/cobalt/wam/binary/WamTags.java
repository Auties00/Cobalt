package com.github.auties00.cobalt.wam.binary;

/**
 * A utility class that defines binary tag constants used in WhatsApp's WAM
 * (WhatsApp Metrics) binary serialization protocol.
 *
 * <p>Each entry in a WAM buffer is encoded as a tag byte followed by a field
 * identifier and an optional value payload. The tag byte packs two orthogonal
 * pieces of information:
 *
 * <ul>
 * <li><b>Role and continuation flags</b> (lower 4 bits): identify whether the
 *     entry is a global attribute, event marker, or event field, and whether
 *     more entries follow in the current group.
 * <li><b>Value-type selector</b> (upper 4 bits): encode the wire
 *     representation of the value (null, compact integer, variable-width
 *     integer, float, or length-prefixed string).
 * </ul>
 *
 * <p>The final tag byte written to the buffer is constructed by bitwise-ORing
 * the applicable role/continuation flags with the value-type constant. For
 * example, a 16-bit integer field that is the last in its event would produce
 * the flags {@code FIELD | LAST | VALUE_INT16}.
 *
 * <p>This class cannot be instantiated as it only contains static constant
 * definitions.
 *
 * @see WamEncoder
 */
public final class WamTags {
    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private WamTags() {
        throw new AssertionError();
    }

    /**
     * Role flag for global attribute entries.
     * This has the wire value of {@code 0x00} in the tag byte.
     */
    public static final int GLOBAL = 0;

    /**
     * Role flag for event marker entries.
     * This has the wire value of {@code 0x01} in the tag byte.
     */
    public static final int EVENT = 1;

    /**
     * Role flag for event field entries.
     * This has the wire value of {@code 0x02} in the tag byte.
     */
    public static final int FIELD = 2;

    /**
     * Termination flag indicating this is the last entry in the current
     * group. When set on an event marker it means no fields follow;
     * when set on a field entry it means no more fields follow in
     * this event.
     * This has the wire value of {@code 0x04} in the tag byte.
     */
    public static final int LAST = 4;

    /**
     * Wide-ID flag indicating the field identifier is encoded as a two-byte
     * uint16 instead of a single byte.
     * This has the wire value of {@code 0x08} in the tag byte.
     */
    public static final int WIDE_ID = 8;

    /**
     * Value-type selector for absent (null) values.
     * No payload bytes follow the tag.
     */
    public static final int VALUE_NULL = 0x00;

    /**
     * Value-type selector for the integer value {@code 0}.
     * No payload bytes follow the tag.
     */
    public static final int VALUE_INT_0 = 0x10;

    /**
     * Value-type selector for the integer value {@code 1}.
     * No payload bytes follow the tag.
     */
    public static final int VALUE_INT_1 = 0x20;

    /**
     * Value-type selector for a signed 8-bit integer.
     * One payload byte follows the tag.
     */
    public static final int VALUE_INT8 = 0x30;

    /**
     * Value-type selector for a signed 16-bit integer in little-endian order.
     * Two payload bytes follow the tag.
     */
    public static final int VALUE_INT16 = 0x40;

    /**
     * Value-type selector for a signed 32-bit integer in little-endian order.
     * Four payload bytes follow the tag.
     */
    public static final int VALUE_INT32 = 0x50;

    /**
     * Value-type selector for a signed 64-bit integer in little-endian order.
     * Eight payload bytes follow the tag.
     */
    public static final int VALUE_INT64 = 0x60;

    /**
     * Value-type selector for an IEEE 754 double-precision float in
     * little-endian order.
     * Eight payload bytes follow the tag.
     */
    public static final int VALUE_FLOAT64 = 0x70;

    /**
     * Value-type selector for a UTF-8 string with a uint8 length prefix.
     * One length byte plus the UTF-8 payload follow the tag.
     */
    public static final int VALUE_STR8 = 0x80;

    /**
     * Value-type selector for a UTF-8 string with a uint16 length prefix
     * in little-endian order.
     * Two length bytes plus the UTF-8 payload follow the tag.
     */
    public static final int VALUE_STR16 = 0x90;

    /**
     * Value-type selector for a UTF-8 string with a uint32 length prefix
     * in little-endian order.
     * Four length bytes plus the UTF-8 payload follow the tag.
     */
    public static final int VALUE_STR32 = 0xA0;
}
