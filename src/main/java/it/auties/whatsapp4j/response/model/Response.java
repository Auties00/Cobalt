package it.auties.whatsapp4j.response.model;

import jakarta.validation.constraints.NotNull;

/**
 * An interface that can be implemented to signal that a class may represent a serialization technique used by WhatsappWeb's WebSocket when sending a request.
 * 
 * This class only allows three types of implementations:
 * <ul>
 * <li>{@link BinaryResponse} - characterized by a WhatsappNode </li>
 * <li>{@link JsonResponseModel} - characterized by a JSON String</li>
 * <li>{@link JsonListResponse} - characterized by a list of objects serialized as a JSON String</li>
 * </ul>
 */
public sealed interface Response permits BinaryResponse, JsonResponse, JsonListResponse {
    /**
     * Converts this object to a ResponseModel
     *
     * @param clazz a Class that represents {@code <T>}
     * @param <T> the specific raw type of the model
     * @return an instance of the type of model requested
     */
   default <T extends ResponseModel> @NotNull T toModel(@NotNull Class<T> clazz) {
       throw new UnsupportedOperationException("To model is not supported on this object");
   }
}