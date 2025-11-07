package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebOnInitialInfoStreamNodeHandler extends SocketStream.Handler {
    public WebOnInitialInfoStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        if(!whatsapp.store().registered()) {
            whatsapp.store()
                    .setRegistered(true);
            whatsapp.store()
                    .serialize();
        }

        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onLoggedIn(whatsapp));
        }
    }
}
