package com.github.auties00.cobalt.registration.push.apns.plist.value;

/**
 * Holds a plist {@code <integer>} leaf, the binary plist {@code 0x10..0x13} marker family covering
 * 1, 2, 4, and 8-byte integer widths.
 *
 * <p>An instance represents a whole number carried by an APNS plist payload, such as a status code,
 * length, or identifier. Every supported on-wire width is narrowed into a single Java {@code long}
 * so consumers do not branch on the encoded size.
 *
 * @implNote This implementation only models widths up to 8 bytes; binary plists also define a
 *           {@code 0x14} 16-byte integer marker, which the parser surfaces as an
 *           {@link java.io.IOException} rather than truncating silently.
 * @param value the integer value
 */
public record PlistIntegerValue(long value) implements PlistValue {
}
