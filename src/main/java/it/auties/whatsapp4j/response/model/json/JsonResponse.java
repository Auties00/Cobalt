package it.auties.whatsapp4j.response.model.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp4j.response.model.common.Response;
import it.auties.whatsapp4j.response.model.common.ResponseModel;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

/**
 * A record that wraps a Map representing a JSON String sent by WhatsappWeb's WebSocket as response for a request.
 * This map of attributes can be converted to a ResponseModel using {@link JsonResponse#toModel(Class)}.
 * If a model is not available or necessary, many getters are available to query data.
 * This class is final, this means that it cannot be extended.
 */
public final class JsonResponse extends Response<Map<String, ?>> {
    public JsonResponse(@NonNull String tag, String description, @NonNull Map<String, ?> content) {
        super(tag, description, content);
    }

    /**
     * Constructs a new instance of JsonResponse from a json string
     *
     * @param json the json string to parse
     * @return a new instance of JsonResponse with the above characteristics
     * @throws IllegalArgumentException if {@code json} cannot be parsed
     */
    public static @NonNull JsonResponse fromJson(@NonNull String json) {
        try {
            var index = json.indexOf("{");
            return new JsonResponse("json", "json", index == -1 ? Map.of() : JACKSON.readValue(json.substring(index), new TypeReference<>() {}));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot deserialize %s into a JsonResponse".formatted(json), ex);
        }
    }

    /**
     * Returns if a key is present in the json that this object wraps
     *
     * @param key the key to search
     * @return true if the key is present
     */
    public boolean hasKey(@NonNull String key) {
        return content.containsKey(key);
    }

    /**
     * Returns an optional String representing the value for the input key
     *
     * @param key the key to search
     * @return a non empty optional if the key is present, otherwise an empty optional
     */
    public @NonNull Optional<String> getString(@NonNull String key) {
        return getObject(key, String.class);
    }

    /**
     * Returns an optional Integer representing the value for the input key
     *
     * @param key the key to search
     * @return a non empty optional if the key is present, otherwise an empty optional
     */
    public @NonNull Optional<Integer> getInteger(@NonNull String key) {
        return getObject(key, Integer.class);
    }

    /**
     * Returns an Integer representing the value for the input key
     *
     * @param key the key to search
     * @return an Integer representing the value for the input key
     * @throws java.util.NoSuchElementException if {@code key} isn't found
     */
    public int getInt(@NonNull String key) {
        return getObject(key, Integer.class).orElseThrow();
    }

    /**
     * Returns an optional Object converted to {@code <T>} representing the value for the input key
     *
     * @param key   the key to search
     * @param clazz a Class that represents {@code <T>}
     * @param <T>   the type of the result
     * @return a non empty optional if the key is present, otherwise an empty optional
     */
    public <T> @NonNull Optional<T> getObject(@NonNull String key, @NonNull Class<T> clazz) {
        return Optional.ofNullable(content.get(key)).map(clazz::cast);
    }

    /**
     * Converts this object to a JsonResponseModel
     *
     * @param clazz a Class that represents {@code <T>}
     * @param <T>   the specific raw type of the model
     * @return an instance of the type of model requested
     * @throws IllegalArgumentException if the content that this object wraps cannot be converted to the specified class
     */
    @Override
    public <T extends ResponseModel> @NonNull T toModel(@NonNull Class<T> clazz) {
        try {
            return JACKSON.convertValue(content, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert JsonResponse with content %s to %s".formatted(content(), clazz.getName()), e);
        }
    }
}
