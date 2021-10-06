package it.auties.whatsapp4j.request;

import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.protobuf.chat.Chat;
import it.auties.whatsapp4j.common.protobuf.contact.Contact;
import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.common.request.JsonRequest;
import it.auties.whatsapp4j.common.response.JsonResponseModel;
import lombok.NonNull;

import java.util.List;

/**
 * A JSON request used to force WhatsappWeb's WebSocket to send updates regarding a contact's status.
 * After this message, the status can be fetched by listening to {@link IWhatsappListener#onContactPresenceUpdate(Chat, Contact)} or {@link Contact#lastKnownPresence()}.
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
