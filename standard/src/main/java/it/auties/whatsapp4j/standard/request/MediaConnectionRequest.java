package it.auties.whatsapp4j.standard.request;

import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.protobuf.chat.Chat;
import it.auties.whatsapp4j.common.protobuf.contact.Contact;
import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.request.JsonRequest;
import it.auties.whatsapp4j.common.response.JsonResponseModel;
import lombok.NonNull;

import java.util.List;

/**
 * A JSON request used to force WhatsappWeb's WebSocket to send updates regarding a contact's status.
 * After this message, the status can be fetched by listening to {@link IWhatsappListener#onContactPresenceUpdate(Chat, Contact)} or {@link Contact#lastKnownPresence()}.
 */
public abstract class MediaConnectionRequest<M extends JsonResponseModel> extends JsonRequest<M> {
    public MediaConnectionRequest(@NonNull WhatsappConfiguration configuration) {
        super(configuration);
    }

    @Override
    public @NonNull List<Object> buildBody() {
        return List.of("query", "mediaConn");
    }
}