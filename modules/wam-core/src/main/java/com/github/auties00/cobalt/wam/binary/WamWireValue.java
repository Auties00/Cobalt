package com.github.auties00.cobalt.wam.binary;

import java.util.NavigableMap;

/**
 * A decoded WAM field value in its on-wire representation.
 *
 * <p>The WAM tag byte distinguishes only how a value is represented on the
 * wire, not its semantic {@link com.github.auties00.cobalt.wam.model.WamType}:
 * {@code INTEGER}, {@code BOOLEAN}, {@code TIMER}, and {@code ENUM} all travel
 * as integers, {@code FLOAT} as an IEEE 754 float64, and {@code STRING} as a
 * length-prefixed UTF-8 sequence. This sealed hierarchy mirrors those three
 * wire forms so that a field collection can be decoded, sized, and encoded
 * generically through a single pattern match, with no per-field type table.
 * Semantic interpretation (for example reading an integer back as an
 * {@link java.time.Instant} or an enum constant) is applied by the typed
 * accessors of each generated event implementation.
 *
 * @see WamEventDecoder#readFields(WamEventDecoder, boolean)
 * @see WamEventEncoder#writeEvent(int, int, NavigableMap)
 * @see WamEventSizes#sizeOf(int, int, java.util.Map)
 */
public sealed interface WamWireValue {
    /**
     * An integer wire value, covering the {@code INTEGER}, {@code BOOLEAN},
     * {@code TIMER}, and {@code ENUM} semantic types, all of which travel as
     * a variable-width integer.
     *
     * @param value the decoded integer, widened to {@code long}
     */
    record WamInt(long value) implements WamWireValue {}

    /**
     * A double-precision floating-point wire value.
     *
     * @param value the decoded {@code float64} value
     */
    record WamFloat(double value) implements WamWireValue {}

    /**
     * A length-prefixed UTF-8 string wire value.
     *
     * @param value the decoded string, never {@code null}
     */
    record WamString(String value) implements WamWireValue {}
}
