package it.auties.whatsapp4j.response.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import jakarta.validation.constraints.NotNull;

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
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(JsonListResponse.class),
                @JsonSubTypes.Type(BinaryResponse.class),
                @JsonSubTypes.Type(JsonResponseModel.class)
        }
)
public interface Response<J extends Response<J, T>, T extends ResponseModel<T>> {
    /**
     * Converts this object to a ResponseModel
     *
     * @param clazz a Class that represents {@code <T>}
     * @return an instance of the type of model requested
     */
    default @NotNull T toModel(@NotNull Class<T> clazz) {
        throw new UnsupportedOperationException("This method is not supported in this response");
    }
}