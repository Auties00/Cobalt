package it.auties.whatsapp4j.request.impl;

import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.request.model.JsonRequest;
import it.auties.whatsapp4j.response.impl.json.DiscardResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A JSON request used to force WhatsappWeb's WebSocket to send updates regarding a contact's status
 * After this message, the status can be fetched by listening to {@link WhatsappListener#onContactPresenceUpdate(WhatsappChat, WhatsappContact)} or {@link WhatsappContact#lastKnownPresence()}
 */
public abstract class SubscribeUserPresenceRequest extends JsonRequest<DiscardResponse> {
    private final @NotNull String jid;

    public SubscribeUserPresenceRequest(@NotNull WhatsappConfiguration configuration, @NotNull String jid) {
        super(configuration);
        this.jid = jid;
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("action", "presence", "subscribe", jid);
    }
}
