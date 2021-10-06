package it.auties.whatsapp4j.request;

import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.request.JsonRequest;
import it.auties.whatsapp4j.response.DiscardResponse;
import lombok.NonNull;

import java.util.List;

/**
 * A JSON request used to signal to WhatsappWeb's WebSocket that this session's keys should be invalidated and the session closed
 */
public abstract class LogOutRequest extends JsonRequest<DiscardResponse> {
    public LogOutRequest(@NonNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    @Override
    public @NonNull List<Object> buildBody() {
        return List.of("admin", "Conn", "disconnect");
    }
}
