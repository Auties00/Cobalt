package it.auties.whatsapp4j.response.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A record that wraps a List of Objects sent by WhatsappWeb's WebSocket as response for a request.
 * This List of Objects cannot be safely converted to a ResponseModel, using {@link JsonListResponse#toModel(Class)} will always throw an exception.
 * This class is final, this means that it cannot be extended.
 */
@Accessors(chain = true)
@Getter
@Setter
public final class JsonListResponse<J extends JsonListResponse<J,T>,T extends ResponseModel<T>>
        implements Response<J,T> {

    private final List<Object> data;

    public JsonListResponse(@NotNull List<Object> data) {
        this.data = data;
    }
}
