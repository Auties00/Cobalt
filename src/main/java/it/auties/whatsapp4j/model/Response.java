package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Response(Map<String, Object> data) {
    private static final ObjectReader JACKSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .reader();

    public static @NotNull Response fromJson(@NotNull String json) throws JsonProcessingException {
        var index = json.indexOf("{");
        return index == -1 ? new Response(new HashMap<>()) : new Response(JACKSON.forType(new TypeReference<>() {}).readValue(json.substring(index)));
    }

    public String getString(@NotNull String key){
        return getObject(key, String.class).orElseThrow();
    }

    public String getNullableString(@NotNull String key){
        return getObject(key, String.class).orElse(null);
    }

    public Integer getNullableInteger(@NotNull String key){
        return getObject(key, Integer.class).orElse(null);
    }

    public int getNumber(@NotNull String key){
        return getObject(key, Integer.class).orElseThrow();
    }

    public <T> Optional<T> getObject(@NotNull String key, @NotNull Class<T> clazz){
        return Optional.ofNullable(data().get(key)).map(clazz::cast);
    }
}