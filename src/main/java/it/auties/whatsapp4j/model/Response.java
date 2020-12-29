package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public record Response(Map<String, String> data) {
    private static final ObjectReader JACKSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .reader();

    public static @NotNull Response fromJson(@NotNull String json) throws JsonProcessingException {
        return new Response(JACKSON
                .forType(new TypeReference<>() {})
                .readValue(json.substring(json.indexOf("{"))));
    }

    public Optional<String> getValue(@Nullable String key){
        if(key == null) return Optional.empty();
        return Optional.ofNullable(data().get(key));
    }
}