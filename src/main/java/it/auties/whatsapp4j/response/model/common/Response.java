package it.auties.whatsapp4j.response.model.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.response.model.json.JsonListResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Optional;

/**
 * An interface that can be implemented to signal that a class may represent a serialization technique used by WhatsappWeb's WebSocket when sending a request.
 * <p>
 * This class only allows three types of implementations:
 * <ul>
 * <li>{@link BinaryResponse} - characterized by a WhatsappNode </li>
 * <li>{@link JsonResponseModel} - characterized by a JSON String</li>
 * <li>{@link JsonListResponse} - characterized by a list of objects serialized as a JSON String</li>
 * </ul>
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true, chain = true)
public abstract sealed class Response<C> permits BinaryResponse, JsonResponse, JsonListResponse {
    protected static final ObjectMapper JACKSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    protected @NonNull String tag;
    protected String description;
    protected @NonNull C content;

    /**
     * Constructs a new instance of WhatsappResponse from a json string
     *
     * @param parse the json string to parse
     * @return a new instance of WhatsappResponse with the above characteristics
     * @throws IllegalArgumentException if {@code parse} cannot be parsed
     */
    public static @NonNull Response<?> fromTaggedResponse(@NonNull String parse) {
        try {
            var split = parse.split(",", 2);
            if (split.length != 2 && parse.startsWith("!")) {
                return new JsonResponse(parse, "pong", new HashMap<>());
            }

            var tag = split[0];
            var content = parseContent(split[1], 0);
            if (content.isEmpty()) {
                return new JsonResponse(tag, null, new HashMap<>());
            }

            var jsonNode = JACKSON.readTree(content);
            if (!jsonNode.isArray()) {
                return new JsonResponse(tag, null, JACKSON.readerFor(new TypeReference<>() {}).readValue(jsonNode));
            }

            var possibleMap = Optional.ofNullable(jsonNode.get(1)).map(JsonNode::toString).orElse("");
            if (!possibleMap.startsWith("{") || !possibleMap.endsWith("}")) {
                return new JsonListResponse(tag, null, JACKSON.readerFor(new TypeReference<>() {}).readValue(jsonNode));
            }

            return new JsonResponse(tag, jsonNode.get(0).textValue(), JACKSON.readerFor(new TypeReference<>() {}).readValue(possibleMap));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot decode Response %s with error %s".formatted(parse, e.getMessage()));
        }
    }

    private static @NonNull String parseContent(@NonNull String content, int index) {
        return content.length() > index && content.charAt(index) == ',' ? parseContent(content, index + 1) : content.substring(index);
    }

    /**
     * Converts this object to a ResponseModel
     *
     * @param clazz a Class that represents {@code <T>}
     * @param <T>   the specific raw type of the model
     * @return an instance of the type of model requested
     */
    public <T extends ResponseModel> @NonNull T toModel(@NonNull Class<T> clazz) {
        throw new UnsupportedOperationException("To model is not supported on this object");
    }
}