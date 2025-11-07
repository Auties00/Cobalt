package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.AUTH;

public final class WebNotifyStoreStreamNodeHandler extends SocketStream.Handler {
    public WebNotifyStoreStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        try {
            whatsapp.store()
                    .serializer()
                    .finishDeserialize(whatsapp.store());
            if(whatsapp.store().syncedChats()) {
                var chats = whatsapp.store().chats();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onChats(whatsapp, chats));
                }
            }
            if(whatsapp.store().syncedContacts()) {
                var contacts = whatsapp.store().contacts();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onContacts(whatsapp, contacts));
                }
            }
            if(whatsapp.store().syncedNewsletters()) {
                var newsletters = whatsapp.store().newsletters();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onNewsletters(whatsapp, newsletters));
                }
            }
            if(whatsapp.store().syncedStatus()) {
                var status = whatsapp.store().status();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onStatus(whatsapp, status));
                }
            }
        } catch (Exception exception) {
            whatsapp.handleFailure(AUTH, exception);
        }
    }
}
