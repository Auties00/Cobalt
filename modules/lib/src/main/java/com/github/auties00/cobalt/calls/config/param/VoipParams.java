package com.github.auties00.cobalt.calls.config.param;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * Holds one parsed voip param set as a wire path keyed value map.
 *
 * <p>This is the in memory materialisation of a single {@code <voip_settings>} document. The set keeps
 * a sparse map from {@link VoipParamKey} (identified by its area sectioned {@code section.key} wire
 * path) to the raw value the document carried, holding only the tunables that were actually present. A
 * leaf whose wire path is modelled is keyed by its catalogue key; a leaf whose wire path is not
 * modelled is keyed by an {@linkplain VoipParamKey#unknown(String) unknown} key, so no parsed value is
 * dropped.
 *
 * <p>The {@code <voip_settings>} document carries every leaf as a JSON string ({@code "60"},
 * {@code "1.0"}, {@code "true"}), and the dynamic rate control engine writes back boxed scalars, so a
 * stored value may be a {@link String}, a {@link Number}, a {@link Boolean}, a {@code long[]}, or a
 * {@code double[]}. The typed accessors therefore coerce on read: a numeric string or number reads as
 * an integer or a floating point value, a {@code "true"}/{@code "1"}/nonzero value reads as a
 * boolean, and a mismatch reports {@linkplain Optional#empty() absent}.
 *
 * <p>A set is mutable: the dynamic rate control rule engine ({@link DynVoipParamUpdater}) overwrites
 * individual values on the live set each round, and the manager copies a stored set before mutating it
 * so the stored baseline is never clobbered. The backing map is a plain {@link HashMap}: a set is
 * confined to one thread at a time (built by the deserializer, then copied and mutated only under the
 * owning {@link LiveVoipParamManager}'s lock), so it is never accessed concurrently.
 *
 * @implNote This implementation stores only the tunables present in the document as a sparse
 * {@link HashMap} keyed by the document's own {@code section.key} wire path, rather than materialising
 * a dense fixed layout struct over the full parameter catalogue.
 */
public final class VoipParams {
    /**
     * The parsed tunables, keyed by their {@link VoipParamKey}, holding the raw document value.
     */
    private final Map<VoipParamKey, Object> values;

    /**
     * Constructs an empty voip param set.
     *
     * <p>A freshly constructed set holds no values; the deserializer populates it as it walks the
     * JSON document.
     */
    public VoipParams() {
        this.values = new HashMap<>();
    }

    /**
     * Constructs a voip param set as a mostly deep copy of another set.
     *
     * <p>The new set carries the same key to value bindings as the source. Scalar values are
     * immutable boxed types and are shared directly; array values are cloned so an override applied to
     * the copy cannot mutate the source's array.
     *
     * @param source the set to copy
     */
    public VoipParams(VoipParams source) {
        this.values = HashMap.newHashMap(source.values.size());
        for (var entry : source.values.entrySet()) {
            values.put(entry.getKey(), copyValue(entry.getValue()));
        }
    }

    /**
     * Returns a copy of one stored value, cloning array values and sharing scalar values.
     *
     * @param value the stored value to copy
     * @return a copy safe to mutate independently of the source
     */
    private static Object copyValue(Object value) {
        return switch (value) {
            case long[] array -> array.clone();
            case double[] array -> array.clone();
            default -> value;
        };
    }

    /**
     * Stores a raw value under the given key.
     *
     * <p>The value is the JSON leaf the document carried (a {@link String} or {@link Number}), a
     * boxed scalar written by the dynamic rate control engine, or an array; the typed accessors coerce
     * it on read. An array value is stored by reference; callers must not retain and mutate the passed
     * array after this call.
     *
     * @param key   the key to set
     * @param value the raw value to store
     */
    public void put(VoipParamKey key, Object value) {
        values.put(key, value);
    }

    /**
     * Stores an integer value under the given key.
     *
     * @param key   the key to set
     * @param value the integer value to store
     */
    public void putInteger(VoipParamKey key, long value) {
        values.put(key, value);
    }

    /**
     * Stores a floating point value under the given key.
     *
     * @param key   the key to set
     * @param value the floating point value to store
     */
    public void putDouble(VoipParamKey key, double value) {
        values.put(key, value);
    }

    /**
     * Stores a string value under the given key.
     *
     * @param key   the key to set
     * @param value the string value to store
     */
    public void putString(VoipParamKey key, String value) {
        values.put(key, value);
    }

    /**
     * Stores an integer array value under the given key.
     *
     * <p>The array is stored by reference; callers must not retain and mutate the passed array after
     * this call.
     *
     * @param key   the key to set
     * @param value the integer array value to store
     */
    public void putIntegerArray(VoipParamKey key, long[] value) {
        values.put(key, value);
    }

    /**
     * Stores a floating point array value under the given key.
     *
     * <p>The array is stored by reference; callers must not retain and mutate the passed array after
     * this call.
     *
     * @param key   the key to set
     * @param value the floating point array value to store
     */
    public void putDoubleArray(VoipParamKey key, double[] value) {
        values.put(key, value);
    }

    /**
     * Returns the integer value stored under the given key, coercing the raw value on read.
     *
     * <p>A {@link Number} reads as its {@code long} value, a numeric {@link String} is parsed (an
     * integral string directly, a decimal string truncated toward zero), and a {@link Boolean} reads
     * as {@code 1} or {@code 0}. A value that is unset or cannot be parsed as a number reports
     * {@link OptionalLong#empty()}.
     *
     * @param key the key to read
     * @return the integer value, or {@link OptionalLong#empty()} if unset or not numeric
     */
    public OptionalLong getInteger(VoipParamKey key) {
        return switch (values.get(key)) {
            case Number value -> OptionalLong.of(value.longValue());
            case Boolean value -> OptionalLong.of(value ? 1L : 0L);
            case String value -> parseLong(value);
            case null, default -> OptionalLong.empty();
        };
    }

    /**
     * Parses a numeric string to a long, truncating a decimal string toward zero.
     *
     * @param value the string to parse
     * @return the parsed value, or {@link OptionalLong#empty()} if it is not a number
     */
    private static OptionalLong parseLong(String value) {
        try {
            return OptionalLong.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            try {
                return OptionalLong.of((long) Double.parseDouble(value.trim()));
            } catch (NumberFormatException alsoIgnored) {
                return OptionalLong.empty();
            }
        }
    }

    /**
     * Returns the floating point value stored under the given key, coercing the raw value on read.
     *
     * <p>A {@link Number} reads as its {@code double} value and a numeric {@link String} is parsed. A
     * value that is unset or cannot be parsed as a number reports {@link OptionalDouble#empty()}.
     *
     * @param key the key to read
     * @return the floating point value, or {@link OptionalDouble#empty()} if unset or not numeric
     */
    public OptionalDouble getDouble(VoipParamKey key) {
        return switch (values.get(key)) {
            case Number value -> OptionalDouble.of(value.doubleValue());
            case String value -> parseDouble(value);
            case null, default -> OptionalDouble.empty();
        };
    }

    /**
     * Parses a numeric string to a double.
     *
     * @param value the string to parse
     * @return the parsed value, or {@link OptionalDouble#empty()} if it is not a number
     */
    private static OptionalDouble parseDouble(String value) {
        try {
            return OptionalDouble.of(Double.parseDouble(value.trim()));
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Returns the string value stored under the given key.
     *
     * <p>A stored {@link String} reads as itself; any other stored shape reports
     * {@link Optional#empty()} rather than its textual form, so a caller asking for a string gets one
     * only when the document carried a string leaf.
     *
     * @param key the key to read
     * @return the string value, or {@link Optional#empty()} if unset or not a string
     */
    public Optional<String> getString(VoipParamKey key) {
        return values.get(key) instanceof String value ? Optional.of(value) : Optional.empty();
    }

    /**
     * Returns the boolean value stored under the given key, coercing the raw value on read.
     *
     * <p>A flag is carried as a JSON string ({@code "true"} or {@code "1"}) or a number, so both are
     * coerced: a {@code "true"} (ignoring case) or {@code "1"} string, a nonzero number, and a
     * {@link Boolean} {@code true} all read as {@code true}; every other string and a zero number read
     * as {@code false}. A value that is unset or of an uncoercible shape reports
     * {@link Optional#empty()}.
     *
     * @param key the key to read
     * @return the boolean value, or {@link Optional#empty()} if unset or uncoercible
     */
    public Optional<Boolean> getBoolean(VoipParamKey key) {
        return switch (values.get(key)) {
            case Boolean value -> Optional.of(value);
            case Number value -> Optional.of(value.longValue() != 0L);
            case String value -> Optional.of(value.equalsIgnoreCase("true") || value.equals("1"));
            case null, default -> Optional.empty();
        };
    }

    /**
     * Returns the integer array value stored under the given key.
     *
     * <p>The returned array is a defensive clone; mutating it does not change the stored value.
     *
     * @param key the key to read
     * @return the integer array value, or {@link Optional#empty()} if unset or not an integer array
     */
    public Optional<long[]> getIntegerArray(VoipParamKey key) {
        return values.get(key) instanceof long[] value ? Optional.of(value.clone()) : Optional.empty();
    }

    /**
     * Returns the floating point array value stored under the given key.
     *
     * <p>The returned array is a defensive clone; mutating it does not change the stored value.
     *
     * @param key the key to read
     * @return the floating point array value, or {@link Optional#empty()} if unset or not a
     *         floating point array
     */
    public Optional<double[]> getDoubleArray(VoipParamKey key) {
        return values.get(key) instanceof double[] value ? Optional.of(value.clone()) : Optional.empty();
    }

    /**
     * Returns whether a value is stored under the given key.
     *
     * @param key the key to test
     * @return {@code true} if the key holds a value, {@code false} otherwise
     */
    public boolean contains(VoipParamKey key) {
        return values.containsKey(key);
    }

    /**
     * Returns the number of tunables currently held.
     *
     * @return the count of set keys
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns whether this set holds no values.
     *
     * @return {@code true} if the set is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
