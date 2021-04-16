package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.impl.DiscardResponse;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A JSON request used to signal to WhatsappWeb's WebSocket that this session's keys should be invalidated and the session closed
 */
public class LogOutRequest extends JsonRequest<LogOutRequest,DiscardResponse> {
    public LogOutRequest(@NotNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "Conn", "disconnect");
    }
}
