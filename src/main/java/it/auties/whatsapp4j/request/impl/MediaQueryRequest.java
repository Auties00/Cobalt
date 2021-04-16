package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A JSON request used to force WhatsappWeb's WebSocket to send updates regarding a contact's status.
 * After this message, the status can be fetched by listening to {@link WhatsappListener#onContactPresenceUpdate(WhatsappChat, WhatsappContact)} or {@link WhatsappContact#lastKnownPresence()}.
 */
public abstract class MediaQueryRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    public MediaQueryRequest(@NotNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("query", "mediaConn");
    }
}
