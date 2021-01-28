package it.auties.whatsapp4j.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public record MapResponse(@NotNull Map<String, Object> data) implements Response {
    public boolean hasKey(@NotNull String key){
        return data.containsKey(key);
    }

    public Optional<String> getString(@NotNull String key){
        return getObject(key, String.class);
    }

    public Optional<Integer> getInteger(@NotNull String key){
        return getObject(key, Integer.class);
    }

    public int getInt(@NotNull String key){
        return getObject(key, Integer.class).orElseThrow();
    }

    public <T> Optional<T> getObject(@NotNull String key, @NotNull Class<T> clazz){
        return Optional.ofNullable(data().get(key)).map(clazz::cast);
    }

    public <T> @NotNull T toModel(Class<T> clazz) {
        return JACKSON.convertValue(data, clazz);
    }
}
