package com.github.auties00.cobalt.registration.push.apns.plist.value;

/**
 * Holds a plist {@code <real>} leaf, the binary plist {@code 0x22} (float32) and {@code 0x23}
 * (float64) markers.
 *
 * <p>An instance represents a fractional number carried by an APNS plist payload. Both encoded
 * widths are surfaced as a single {@code double}: a float32 source is widened to {@code double}
 * losslessly, so the decoded tree always exposes the same scalar type regardless of the on-wire
 * precision.
 *
 * @implNote This implementation always stores a {@code double}; a separate float-typed variant
 *           would force consumers to pattern-match on two record types that semantically represent
 *           the same value.
 * @param value the floating-point value
 */
public record PlistFloatingPointValue(double value) implements PlistValue {
}
