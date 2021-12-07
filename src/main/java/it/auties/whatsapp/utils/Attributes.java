package it.auties.whatsapp.utils;

import it.auties.whatsapp.protobuf.contact.ContactId;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Attributes(Map<String, Object> map) {
    public static Attributes empty(){
        return of(Map.of());
    }

    public static Attributes of(@NonNull Map<String, Object> map){
        return new Attributes(new HashMap<>(map));
    }

    public <T> T get(@NonNull String key, @NonNull T defaultValue, @NonNull Class<T> clazz){
        return get(key, clazz)
                .orElse(defaultValue);
    }

    public <T> Optional<T> get(@NonNull String key, @NonNull Class<T> clazz){
        return Optional.ofNullable(map.get(key))
                .map(clazz::cast);
    }

    public long getLong(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseNumber)
                .orElse(0L);
    }

    private long parseNumber(Object value) {
        return switch (value) {
            case Number number -> number.longValue();
            case String string -> Long.parseLong(string);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    public String getString(@NonNull String key){
        return getString(key, "unknown");
    }

    public String getString(@NonNull String key, String defaultValue){
        return get(key, Object.class)
                .map(Object::toString)
                .orElse(defaultValue);
    }

    public Optional<ContactId> getJid(@NonNull String key){
        return get(key, Object.class)
                .map(this::parseJid);

    }

    private ContactId parseJid(Object value) {
        return switch (value) {
            case ContactId jid -> jid;
            case String encodedJid -> ContactId.of(encodedJid);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
