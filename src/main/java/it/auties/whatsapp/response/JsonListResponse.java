package it.auties.whatsapp.response;

import lombok.NonNull;

import java.util.List;

/**
 * A record that wraps a List of Objects sent by WhatsappWeb's WebSocket as response for a request.
 * This List of Objects cannot be safely converted to a ResponseModel, using {@link JsonListResponse#toModel(Class)} will always throw an exception.
 * This class is final, this means that it cannot be extended.
 */
public final class JsonListResponse extends Response<List<Object>> {
    public JsonListResponse(@NonNull String tag, String description, @NonNull List<Object> content) {
        super(tag, description, content);
    }
}
