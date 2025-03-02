package it.auties.whatsapp.model.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.whatsapp.model.jid.Jid;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNull;

/**
 * A utility class that wraps a map and provides easy methods to interact with its content
 *
 * @param toMap the non-null wrapped map
 */
public record Attributes(@JsonValue LinkedHashMap<String, Object> toMap) {
    /**
     * Constructs a new map using the non-null provided entries
     *
     * @param entries the non-null entries
     * @return a new instance of Attributes
     */
    @SafeVarargs
    public static Attributes of(Entry<String, ?>... entries) {
        return ofNullable(ofEntries(entries));
    }

    /**
     * Constructs a new map using the non-null provided entries
     *
     * @param entries the non-null entries
     * @return a new instance of Attributes
     */
    @SafeVarargs
    @JsonCreator
    public static Attributes ofNullable(Entry<String, ?>... entries) {
        return entries == null ? of() : ofNullable(ofEntries(entries));
    }

    /**
     * Constructs a new map using the provided nullable map
     *
     * @param map the nullable existing map
     * @return a new instance of Attributes
     */
    public static Attributes ofNullable(Map<String, ?> map) {
        var modifiableMap = Optional.ofNullable(map)
                .map(LinkedHashMap<String, Object>::new)
                .orElseGet(LinkedHashMap::new);
        return new Attributes(modifiableMap);
    }

    /**
     * Constructs a new map using the provided non-null map
     *
     * @param map the non-null existing map
     * @return a new instance of Attributes
     */
    public static Attributes of(Map<String, ?> map) {
        return ofNullable(Objects.requireNonNull(map));
    }

    /**
     * Checks whether a non-null key has a value in this map
     *
     * @param key the non-null key
     * @return a boolean
     */
    public boolean hasKey(String key) {
        return toMap.containsKey(key);
    }

    /**
     * Checks whether a non-null key value in this map and has the provided value
     *
     * @param key   the non-null key
     * @param value the nullable value to check against
     * @return a boolean
     */
    public boolean hasValue(String key, String value) {
        return Objects.equals(toMap.get(key), value);
    }

    /**
     * Inserts a key-value pair in the wrapped map
     *
     * @param key        the non-null key
     * @param value      the nullable value
     * @param conditions the non-null conditions that must be met to insert the value
     * @param <T>        the type of the value
     * @return the calling instance
     */
    @SafeVarargs
    public final <T> Attributes put(String key, T value, Function<T, Boolean>... conditions) {
        var translated = Arrays.stream(conditions)
                .map(condition -> (BooleanSupplier) () -> condition.apply(value))
                .toArray(BooleanSupplier[]::new);
        return put(key, value, translated);
    }

    /**
     * Inserts a key-value pair in the wrapped map
     *
     * @param key        the non-null key
     * @param value      the nullable value
     * @param conditions the non-null conditions that must be met to insert the value
     * @return the calling instance
     */
    public Attributes put(String key, Object value, BooleanSupplier... conditions) {
        return put(key, value, Arrays.stream(conditions).allMatch(BooleanSupplier::getAsBoolean));
    }

    /**
     * Inserts a key-value pair in the wrapped map
     *
     * @param key       the non-null key
     * @param value     the nullable value
     * @param condition the condition that must be met to insert the value
     * @return the calling instance
     */
    public Attributes put(String key, Object value, boolean condition) {
        if (value != null && condition) {
            toMap.put(key, value);
        }
        return this;
    }

    /**
     * Inserts a key-value pair in the wrapped map
     *
     * @param key   the non-null key
     * @param value the nullable value
     * @return the calling instance
     */
    public Attributes put(String key, Object value) {
        if(value != null) {
            toMap.put(key, value);
        }
        return this;
    }

    /**
     * Gets a value by key in the wrapped map
     *
     * @param key          the non-null key
     * @param defaultValue the non-null default value
     * @param clazz        the non-null type of the value that is returned
     * @param <T>          the type of the value that is returned
     * @return the non-null value
     */
    public <T> T get(String key, T defaultValue, Class<T> clazz) {
        return get(key, clazz).orElse(defaultValue);
    }

    /**
     * Gets a value by key in the wrapped map
     *
     * @param key   the non-null key
     * @param clazz the non-null type of the value that is returned
     * @param <T>   the type of the value that is returned
     * @return the non-null value
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        return Optional.ofNullable(toMap.get(key)).map(clazz::cast);
    }

    /**
     * Gets a value as an int by key in the wrapped map
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public int getInt(String key) {
        return getOptionalInt(key).orElse(0);
    }

    /**
     * Gets a value as an int by key in the wrapped map
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public OptionalInt getOptionalInt(String key) {
        return get(key, Object.class)
                .stream()
                .mapToInt(this::parseInt)
                .findFirst();
    }

    private int parseInt(Object value) {
        return switch (value) {
            case Number number -> number.intValue();
            case String string -> Integer.parseInt(string);
            case null, default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    /**
     * Gets a value as a long by key in the wrapped map
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public long getLong(String key) {
        return getOptionalLong(key).orElse(0L);
    }

    /**
     * Gets a value as a long by key in the wrapped map
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public OptionalLong getOptionalLong(String key) {
        return get(key, Object.class)
                .stream()
                .mapToLong(this::parseLong)
                .findFirst();
    }

    private long parseLong(Object value) {
        return switch (value) {
            case Number number -> number.longValue();
            case String string -> Long.parseLong(string);
            case null, default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }


    /**
     * Gets a non-null value as a string by key in the wrapped map. If the key doesn't exist,
     * unknown is returned.
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public String getString(String key) {
        return getString(key, "unknown");
    }

    /**
     * Gets a value as a string by key in the wrapped map. If the value is null, defaultValue is
     * returned.
     *
     * @param key the non-null key
     * @return a non-null string
     */
    public String getString(String key, String defaultValue) {
        return get(key, Object.class).map(Object::toString).orElse(defaultValue);
    }

    /**
     * Gets a nullable value as a string by key in the wrapped map
     *
     * @param key the non-null key
     * @return the nullable value
     */
    public String getNullableString(String key) {
        return getString(key, null);
    }

    /**
     * Gets a nullable value as an int by key in the wrapped map
     *
     * @param key the non-null key
     * @return the nullable value
     */
    public Integer getNullableInt(String key) {
        var result = getOptionalInt(key);
        return result.isPresent() ? result.getAsInt() : null;
    }

    /**
     * Gets a nullable value as a long by key in the wrapped map
     *
     * @param key the non-null key
     * @return the nullable value
     */
    public Long getNullableLong(String key) {
        var result = getOptionalLong(key);
        return result.isPresent() ? result.getAsLong() : null;
    }

    /**
     * Gets a non-null value as a string by key in the wrapped map. Throws an exception if the key
     * doesn't exist.
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public String getRequiredString(String key) {
        return requireNonNull(getString(key, null), "Missing required attribute %s".formatted(key));
    }

    /**
     * Gets a non-null value as an int by key in the wrapped map.
     * Throws an exception if the key doesn't exist.
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public int getRequiredInt(String key) {
        return getOptionalInt(key)
                .orElseThrow(() -> new NullPointerException("Missing required attribute %s".formatted(key)));
    }

    /**
     * Gets a non-null value as a long by key in the wrapped map.
     * Throws an exception if the key doesn't exist.
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public long getRequiredLong(String key) {
        return getOptionalLong(key)
                .orElseThrow(() -> new NullPointerException("Missing required attribute %s".formatted(key)));
    }

    /**
     * Gets an optional value as a string by key in the wrapped map
     *
     * @param key the non-null key
     * @return a non-null optional
     */
    public Optional<String> getOptionalString(String key) {
        return Optional.ofNullable(getString(key, null));
    }

    /**
     * Gets a value as a boolean by key in the wrapped map
     *
     * @param key the non-null key
     * @return a boolean
     */
    public boolean getBoolean(String key) {
        return get(key, Object.class).map(this::parseBool).orElse(false);
    }

    private boolean parseBool(Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String string -> Boolean.parseBoolean(string);
            case null, default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }


    /**
     * Gets an optional value as a ContactJid by key in the wrapped map
     *
     * @param key the non-null key
     * @return a non-null optional
     */
    public Optional<Jid> getOptionalJid(String key) {
        return get(key, Object.class)
                .map(this::parseJid);
    }

    /**
     * Gets a required value as a ContactJid by key in the wrapped map
     *
     * @param key the non-null key
     * @return a non-null value
     */
    public Jid getRequiredJid(String key) {
        return get(key, Object.class)
                .map(this::parseJid)
                .orElseThrow(() -> new NullPointerException("Missing required attribute %s".formatted(key)));
    }

    private Jid parseJid(Object value) {
        return switch (value) {
            case Jid jid -> jid;
            case String encodedJid -> Jid.of(encodedJid);
            case null, default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    /**
     * Copies this object
     *
     * @return a non-null instance
     */
    public Attributes copy() {
        return new Attributes(toMap);
    }

    /**
     * Returns whether this object is empty
     *
     * @return a boolean
     */
    public boolean isEmpty() {
        return toMap.isEmpty();
    }

    public Attributes putAll(Map<String, ?> map) {
        return putAll(map.entrySet());
    }

    public Attributes putAll(Collection<? extends Entry<String, ?>> entries) {
        for (var entry : entries) {
            toMap.put(entry.getKey(), entry.getValue());
        }

        return this;
    }

    @SafeVarargs
    public final Attributes putAll(Entry<String, ?>... entries) {
        for (var entry : entries) {
            toMap.put(entry.getKey(), entry.getValue());
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    public Entry<String, Object>[] toEntries() {
        return toMap.entrySet().toArray(Entry[]::new);
    }
}
