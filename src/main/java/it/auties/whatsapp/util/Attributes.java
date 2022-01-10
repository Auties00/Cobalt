package it.auties.whatsapp.util;

import it.auties.whatsapp.protobuf.contact.ContactJid;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record Attributes(Map<String, Object> map) {
    public static Attributes empty(){
        return of(Map.of());
    }

    public static Attributes of(@NonNull Map<String, Object> map){
        return new Attributes(new HashMap<>(map));
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

    public String getRequiredString(@NonNull String key){
        return requireNonNull(getString(key, null));
    }

    public String getString(@NonNull String key, String defaultValue){
        return get(key, Object.class)
                .map(Object::toString)
                .orElse(defaultValue);
    }

    public Optional<ContactJid> getJid(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseJid);

    }

    private ContactJid parseJid(Object value) {
        return switch (value) {
            case ContactJid jid -> jid;
            case String encodedJid -> ContactJid.ofUser(encodedJid);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
