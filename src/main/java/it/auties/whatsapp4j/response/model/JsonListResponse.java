package it.auties.whatsapp4j.response.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A record that wraps a List of Objects sent by WhatsappWeb's WebSocket as response for a request.
 * This List of Objects cannot be safely converted to a ResponseModel, using {@link JsonListResponse#toModel(Class)} will always throw an exception.
 * This class is final, this means that it cannot be extended.
 */
public final record JsonListResponse(@NotNull List<Object> data) implements Response {
    /**
     * This method is not supported
     *
     * @throws UnsupportedOperationException this operation is not supported
     */
    @Override
    public <T extends ResponseModel> @NotNull T toModel(@NotNull Class<T> clazz) {
        throw new UnsupportedOperationException();
    }
}
