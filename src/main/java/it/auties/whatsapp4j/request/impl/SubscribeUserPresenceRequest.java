package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import it.auties.whatsapp4j.whatsapp.WhatsappConfiguration;
import lombok.NonNull;

import java.util.List;

/**
 * A JSON request used to force WhatsappWeb's WebSocket to send updates regarding a contact's status.
 * After this message, the status can be fetched by listening to {@link WhatsappListener#onContactPresenceUpdate(it.auties.whatsapp4j.protobuf.chat.Chat, it.auties.whatsapp4j.protobuf.contact.Contact)} or {@link it.auties.whatsapp4j.protobuf.contact.Contact#lastKnownPresence()}.
 */
public abstract class SubscribeUserPresenceRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    private final @NonNull String jid;

    public SubscribeUserPresenceRequest(@NonNull WhatsappConfiguration configuration, @NonNull String jid) {
        super(configuration);
        this.jid = jid;
    }

    @Override
    public @NonNull List<Object> buildBody() {
        return List.of("action", "presence", "subscribe", jid);
    }
}
