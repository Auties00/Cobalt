package com.github.auties00.cobalt.registration.push.apns.plist.value;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds a plist {@code <data>} leaf as a slice over a backing buffer, the binary plist
 * {@code 0x40..0x4F} marker family.
 *
 * <p>An instance represents opaque bytes carried by an APNS plist payload, such as the FairPlay
 * token or the device push-token blob. The value is the {@code [offset, offset + length)} range of
 * {@code source}; {@link #toByteArray()} returns an isolated copy when the bytes must outlive the
 * parser's source buffer.
 *
 * @implNote This implementation retains the parser's source buffer by reference to allow zero-copy
 *           decoding of binary plists; mutating {@link #source()} after construction would silently
 *           corrupt the stored value.
 * @param source the backing buffer
 * @param offset the start offset within {@code source}
 * @param length the number of bytes in the slice
 */
public record PlistDataValue(byte[] source, int offset, int length) implements PlistValue {
    /**
     * Constructs a data leaf, rejecting a {@code null} buffer and validating the slice bounds.
     *
     * <p>The slice must satisfy {@code 0 <= offset} and {@code offset + length <= source.length},
     * enforced via {@link Objects#checkFromIndexSize(int, int, int)}.
     *
     * @param source the backing buffer
     * @param offset the start offset within {@code source}
     * @param length the number of bytes in the slice
     * @throws NullPointerException      if {@code source} is {@code null}
     * @throws IndexOutOfBoundsException if the slice escapes the buffer
     */
    public PlistDataValue {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
    }

    /**
     * Constructs a data leaf wrapping the whole of {@code bytes} without copying.
     *
     * <p>The slice spans the entire array, from offset {@code 0} for {@code bytes.length} bytes.
     * The array is retained by reference, so the caller must not mutate {@code bytes} after this
     * call returns.
     *
     * @param bytes the source bytes
     * @throws NullPointerException if {@code bytes} is {@code null}
     */
    public PlistDataValue(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Returns a freshly allocated copy of the stored bytes.
     *
     * <p>The copy is independent of the backing buffer, so it may be mutated or persisted beyond
     * the lifetime of the parser source.
     *
     * @return a copy of the slice
     */
    public byte[] toByteArray() {
        return Arrays.copyOfRange(source, offset, offset + length);
    }
}
