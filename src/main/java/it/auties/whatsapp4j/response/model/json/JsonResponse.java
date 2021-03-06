package it.auties.whatsapp4j.response.model.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp4j.response.model.shared.Response;
import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public record JsonResponse(@NotNull Map<String, ?> data) implements Response {
    private static final ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static @NotNull JsonResponse fromJson(@NotNull String json) {
        try {
            var index = json.indexOf("{");
            return new JsonResponse(index == -1 ? Map.of() : JACKSON.readValue(json.substring(index), new TypeReference<>() {}));
        }catch (JsonProcessingException ex){
            throw new IllegalArgumentException("WhatsappAPI: Cannot deserialize %s into a JsonResponse".formatted(json));
        }
    }


    public boolean hasKey(@NotNull String key){
        return data.containsKey(key);
    }

    public @NotNull Optional<String> getString(@NotNull String key){
        return getObject(key, String.class);
    }

    public @NotNull Optional<Integer> getInteger(@NotNull String key){
        return getObject(key, Integer.class);
    }

    public int getInt(@NotNull String key){
        return getObject(key, Integer.class).orElseThrow();
    }

    public <T> @NotNull Optional<T> getObject(@NotNull String key, @NotNull Class<T> clazz){
        return Optional.ofNullable(data().get(key)).map(clazz::cast);
    }

    public <T extends ResponseModel> @NotNull T toModel(@NotNull Class<T> clazz) {
        return JACKSON.convertValue(data, clazz);
    }
}
