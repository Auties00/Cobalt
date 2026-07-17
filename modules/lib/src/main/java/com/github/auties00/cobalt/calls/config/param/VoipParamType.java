package com.github.auties00.cobalt.calls.config.param;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the value type codes a voip param schema descriptor carries for one tunable.
 *
 * <p>Every tunable in the voip param catalogue has a fixed value type, encoded as a single
 * byte at offset {@code +6} of the engine's parameter entry descriptor. That byte tells the
 * deserializer how to read the value out of the JSON document and how the rate control rule
 * engine must encode an override for it. This enum reproduces the five descriptor codes:
 * {@link #INTEGER} is a signed integer whose byte width is given separately by the
 * descriptor's value length (one, two, four, or eight bytes); {@link #FLOAT} is a four byte
 * float; {@link #STRING} is a fixed width character buffer whose byte capacity is the
 * descriptor's value length; {@link #ARRAY} is a homogeneous array whose per element width
 * and element type live in the descriptor's array fields; and {@link #ARRAY_COUNT} is the
 * count marker that pairs immediately after an {@link #ARRAY} entry, carrying the array's
 * runtime element count.
 *
 * <p>The wire codes are contiguous from {@code 1} to {@code 5}: {@code 1} {@link #INTEGER},
 * {@code 2} {@link #FLOAT}, {@code 3} {@link #STRING}, {@code 4} {@link #ARRAY}, {@code 5}
 * {@link #ARRAY_COUNT}. Code {@code 0} is not an engine descriptor code and is modelled
 * separately by {@link #UNKNOWN} for wire only leaves that resolve to no descriptor.
 *
 * <p>The type code determines how the value is read:
 * <ul>
 * <li>{@link #INTEGER} (code {@code 1}) loads a signed integer whose byte width is taken from
 * the descriptor's value length field ({@code 1}, {@code 2}, {@code 4}, or {@code 8} bytes).</li>
 * <li>{@link #FLOAT} (code {@code 2}) loads a single four byte float; there is no eight byte
 * double path.</li>
 * <li>{@link #STRING} (code {@code 3}) reads a fixed width character buffer of the descriptor's
 * value length.</li>
 * <li>{@link #ARRAY} (code {@code 4}) reads a homogeneous array and takes its runtime element
 * count from the immediately following {@link #ARRAY_COUNT} entry, bounded by the capacity
 * {@code value_length / element_width}.</li>
 * <li>{@link #ARRAY_COUNT} (code {@code 5}) carries that runtime element count.</li>
 * </ul>
 *
 * <p>Semantic boolean toggles have no distinct type code; they are integer backed fields of
 * width {@code 1} under code {@code 1}.
 */
public enum VoipParamType {
    /**
     * A signed integer whose byte width ({@code 1}, {@code 2}, {@code 4}, or {@code 8}) is
     * carried by the descriptor's value length field, keyed under code {@code 1}.
     */
    INTEGER(1),

    /**
     * A four byte float, keyed under code {@code 2}.
     */
    FLOAT(2),

    /**
     * A fixed width text buffer, keyed under code {@code 3}.
     */
    STRING(3),

    /**
     * A homogeneous array whose per element width and element type live in the
     * descriptor's array fields, keyed under code {@code 4}.
     */
    ARRAY(4),

    /**
     * The count marker that pairs immediately after an {@link #ARRAY} entry to carry the
     * array's runtime element count, keyed under code {@code 5}.
     */
    ARRAY_COUNT(5),

    /**
     * The type of a wire field with no recovered native descriptor, keyed under code {@code 0}.
     *
     * <p>A {@code <voip_settings>} leaf that resolves to no modelled {@link VoipParamKey} is carried
     * by an {@linkplain VoipParamKey#unknown(String) unknown} key whose type is this constant. It is not one of the five engine
     * descriptor codes ({@code 1} to {@code 5}); it marks a value retained by wire path whose engine
     * value type is unknown, so it is read back through whichever {@link VoipParams} accessor coerces
     * the raw value the deserializer stored.
     */
    UNKNOWN(0);

    /**
     * Resolves an engine value type code to its type, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * type in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, VoipParamType> BY_CODE;

    static {
        var byCode = new HashMap<Integer, VoipParamType>();
        for (var type : values()) {
            if (byCode.put(type.code, type) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_CODE = Map.copyOf(byCode);
    }

    /**
     * The integer value type code the engine stores in the descriptor's type byte.
     */
    private final int code;

    /**
     * Constructs a value type constant bound to its engine type code.
     *
     * @param code the integer value type code the engine stores
     */
    VoipParamType(int code) {
        this.code = code;
    }

    /**
     * Returns the integer value type code the engine stores in the descriptor's type byte.
     *
     * @return the engine value type code
     */
    public int code() {
        return code;
    }

    /**
     * Returns whether this type is a scalar that holds a single value.
     *
     * <p>The scalar types are {@link #INTEGER}, {@link #FLOAT}, and
     * {@link #STRING}; {@link #ARRAY} and {@link #ARRAY_COUNT} are not scalar.
     *
     * @return {@code true} if this type holds a single value, {@code false} otherwise
     */
    public boolean isScalar() {
        return this == INTEGER || this == FLOAT || this == STRING;
    }

    /**
     * Returns the value type whose {@linkplain #code() code} equals the given value.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #values()}.
     * @param code the engine value type code to resolve
     * @return the matching value type, or {@link Optional#empty()} if no type matches
     */
    public static Optional<VoipParamType> ofCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
