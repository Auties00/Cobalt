package it.auties.whatsapp.model.request;

import it.auties.whatsapp.model.contact.ContactJid;
import lombok.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNull;

/**
 * A utility class that wraps a map and provides easy methods to interact with its content
 *
 * @param map the non-null wrapped map
 */
public record Attributes(@NonNull Map<String, Object> map) {
    /**
     * Constructs a new map using the non-null provided entries
     *
     * @param entries the non-null entries
     * @return a new instance of Attributes
     */
    @SafeVarargs
    public static Attributes of(@NonNull Entry<String, Object>... entries) {
        return ofNullable(ofEntries(entries));
    }

    /**
     * Constructs a new map using the provided non-null map
     *
     * @param map the non-null existing map
     * @return a new instance of Attributes
     */
    public static Attributes of(@NonNull Map<String, Object> map) {
        return ofNullable(map);
    }

    /**
     * Constructs a new map using the provided nullable map
     *
     * @param map the nullable existing map
     * @return a new instance of Attributes
     */
    public static Attributes ofNullable(Map<String, Object> map) {
        var modifiableMap = Optional.ofNullable(map)
                .map(HashMap::new)
                .orElseGet(HashMap::new);
        return new Attributes(modifiableMap);
    }

    /**
     * Checks whether a non-null key exists in this map
     *
     * @param key the non-null key
     * @return a boolean
     */
    public boolean hasKey(@NonNull String key) {
        return map.containsKey(key);
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
    public final <T> Attributes put(@NonNull String key, T value, @NonNull Function<T, Boolean>... conditions) {
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
    public Attributes put(@NonNull String key, Object value, @NonNull BooleanSupplier... conditions) {
        if (Arrays.stream(conditions)
                .allMatch(BooleanSupplier::getAsBoolean)) {
            map.put(key, value);
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
    public Attributes put(@NonNull String key, Object value) {
        map.put(key, value);
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
    public <T> T get(@NonNull String key, @NonNull T defaultValue, @NonNull Class<T> clazz) {
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
    public <T> Optional<T> get(@NonNull String key, @NonNull Class<T> clazz) {
        return Optional.ofNullable(map.get(key))
                .map(clazz::cast);
    }

    /**
     * Gets a value as an int by key in the wrapped map
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public int getInt(@NonNull String key) {
        return get(key, Object.class).map(this::parseInt)
                .orElse(0);
    }

    private int parseInt(Object value) {
        return switch (value) {
            case Number number -> number.intValue();
            case String string -> Integer.parseInt(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    /**
     * Gets a value as a long by key in the wrapped map
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public long getLong(@NonNull String key) {
        return get(key, Object.class).map(this::parseLong)
                .orElse(0L);
    }

    private long parseLong(Object value) {
        return switch (value) {
            case Number number -> number.longValue();
            case String string -> Long.parseLong(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    /**
     * Gets a non-null value as a string by key in the wrapped map.
     * If the key doesn't exist, unknown is returned.
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public String getString(@NonNull String key) {
        return getString(key, "unknown");
    }

    /**
     * Gets a nullable value as a string by key in the wrapped map
     *
     * @param key the non-null key
     * @return the nullable value
     */
    public String getNullableString(@NonNull String key) {
        return getString(key, null);
    }

    /**
     * Gets a non-null value as a string by key in the wrapped map.
     * Throws an exception if the key doesn't exist.
     *
     * @param key the non-null key
     * @return the non-null value
     */
    public String getRequiredString(@NonNull String key) {
        return requireNonNull(getString(key, null), "Missing required attribute %s".formatted(key));
    }

    /**
     * Gets an optional value as a string by key in the wrapped map
     *
     * @param key the non-null key
     * @return a non-null optional
     */
    public Optional<String> getOptionalString(@NonNull String key) {
        return Optional.ofNullable(getString(key, null));
    }

    /**
     * Gets a value as a string by key in the wrapped map.
     * If the value is null, defaultValue is returned.
     *
     * @param key the non-null key
     * @return a non-null string
     */
    public String getString(@NonNull String key, String defaultValue) {
        return get(key, Object.class).map(Object::toString)
                .orElse(defaultValue);
    }

    /**
     * Gets a value as a boolean by key in the wrapped map
     *
     * @param key the non-null key
     * @return a boolean
     */
    public boolean getBoolean(@NonNull String key) {
        return get(key, Object.class).map(this::parseBool)
                .orElse(false);
    }

    private boolean parseBool(Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String string -> Boolean.parseBoolean(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    /**
     * Gets an optional value as a ContactJid by key in the wrapped map
     *
     * @param key the non-null key
     * @return a non-null optional
     */
    public Optional<ContactJid> getJid(@NonNull String key) {
        return get(key, Object.class).map(this::parseJid);
    }

    private ContactJid parseJid(Object value) {
        return switch (value) {
            case ContactJid jid -> jid;
            case String encodedJid -> ContactJid.of(encodedJid);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
