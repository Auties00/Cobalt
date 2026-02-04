package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class MobileFinishLoginStreamNodeHandler extends SocketStream.Handler {
    public MobileFinishLoginStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        var store = whatsapp.store();
        if (store.jid().isEmpty()) {
            store.phoneNumber().ifPresent(number -> store.setJid(Jid.of(number)));
        }

        if(!store.registered()) {
            store.setRegistered(true);
            store.serialize();
        }

        for(var listener : store.listeners()) {
            Thread.startVirtualThread(() -> listener.onLoggedIn(whatsapp));
        }
    }
}
