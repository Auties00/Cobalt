package com.github.auties00.cobalt.registration.push.apns.plist.value;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds a plist {@code <array>} stanza, the binary plist {@code 0xA0..0xAF} marker family.
 *
 * <p>An instance represents an ordered sequence of nested {@link PlistValue} entries carried by an
 * APNS plist payload; the entries may be homogeneous or mixed. Iteration order matches the on-wire
 * order so that re-encoding the decoded tree reproduces the captured bytes and any signature
 * recomputed over it stays bit-identical.
 *
 * @implNote This implementation stores the supplied {@link List} by reference and exposes an
 *           unmodifiable view through {@link #items()}, avoiding the per-construction defensive copy
 *           that would otherwise inflate parsing of large arrays.
 * @param items the contained values
 */
public record PlistArrayValue(List<PlistValue> items) implements PlistValue {
    /**
     * Constructs an array stanza, rejecting a {@code null} backing list.
     *
     * <p>The list itself must be non-{@code null}; individual entries may be any concrete
     * {@link PlistValue}.
     *
     * @param items the contained values
     * @throws NullPointerException if {@code items} is {@code null}
     */
    public PlistArrayValue {
        Objects.requireNonNull(items, "items");
    }

    /**
     * Returns an unmodifiable view of the backing list.
     *
     * <p>The returned list is a read-only window onto the stored entries; iteration order matches
     * the order observed in the source bytes. Mutating the underlying list, held privately by the
     * parser that built this record, is neither supported nor exposed through this view.
     *
     * @return an unmodifiable list of the contained values
     */
    @Override
    public List<PlistValue> items() {
        return Collections.unmodifiableList(items);
    }
}
