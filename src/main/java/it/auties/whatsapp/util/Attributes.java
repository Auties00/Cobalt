package it.auties.whatsapp.util;

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

public record Attributes(Map<String, Object> map) {
    public static Attributes empty(){
        return of(new HashMap<>());
    }

    @SafeVarargs
    public static Attributes of(Entry<String, Object>... entries){
        return of(ofEntries(entries));
    }

    public static Attributes of(Map<String, Object> map){
        return new Attributes(map != null ? new HashMap<>(map) : new HashMap<>());
    }

    public boolean hasKey(@NonNull String key){
        return map.containsKey(key);
    }

    @SafeVarargs
    public final <T> Attributes put(@NonNull String key, T value, @NonNull Function<T, Boolean>... conditions){
        var translated = Arrays.stream(conditions)
                .<BooleanSupplier>map(condition -> () -> condition.apply(value))
                .toArray(BooleanSupplier[]::new);
        return put(key, value, translated);
    }

    public Attributes put(@NonNull String key, Object value, @NonNull BooleanSupplier... conditions){
        if(Arrays.stream(conditions).allMatch(BooleanSupplier::getAsBoolean)){
            map.put(key, value);
        }

        return this;
    }

    public Attributes put(@NonNull String key, Object value){
        map.put(key, value);
        return this;
    }

    public <T> T get(@NonNull String key, @NonNull T defaultValue, @NonNull Class<T> clazz){
        return get(key, clazz)
                .orElse(defaultValue);
    }

    public <T> Optional<T> get(@NonNull String key, @NonNull Class<T> clazz){
        return Optional.ofNullable(map.get(key))
                .map(clazz::cast);
    }

    public int getInt(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseInt)
                .orElse(0);
    }

    private int parseInt(Object value) {
        return switch (value) {
            case Number number -> number.intValue();
            case String string -> Integer.parseInt(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    public long getLong(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseLong)
                .orElse(0L);
    }

    private long parseLong(Object value) {
        return switch (value) {
            case Number number -> number.longValue();
            case String string -> Long.parseLong(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    public String getString(@NonNull String key){
        return getString(key, "unknown");
    }

    public String getNullableString(@NonNull String key){
        return getString(key, null);
    }

    public String getRequiredString(@NonNull String key){
        return requireNonNull(getString(key, null), "Missing required attribute %s"
                .formatted(key));
    }

    public Optional<String> getOptionalString(@NonNull String key){
        return Optional.ofNullable(getString(key, null));
    }

    public String getString(@NonNull String key, String defaultValue){
        return get(key, Object.class)
                .map(Object::toString)
                .orElse(defaultValue);
    }

    public boolean getBool(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseBool)
                .orElse(false);
    }

    private boolean parseBool(Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String string -> Boolean.parseBoolean(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    public Optional<ContactJid> getJid(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseJid);

    }

    private ContactJid parseJid(Object value) {
        return switch (value) {
            case ContactJid jid -> jid;
            case String encodedJid -> ContactJid.of(encodedJid);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
