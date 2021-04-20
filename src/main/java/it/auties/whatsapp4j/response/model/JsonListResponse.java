package it.auties.whatsapp4j.response.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A record that wraps a List of Objects sent by WhatsappWeb's WebSocket as response for a request.
 * This List of Objects cannot be safely converted to a ResponseModel, using {@link JsonListResponse#toModel(Class)} will always throw an exception.
 * This class is final, this means that it cannot be extended.
 */
public final record JsonListResponse(@NotNull List<Object> data) implements Response {

}
