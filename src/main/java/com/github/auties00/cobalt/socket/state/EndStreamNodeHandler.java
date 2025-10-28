package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class EndStreamNodeHandler extends SocketStream.Handler {
    public EndStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "xmlstreamend");
    }

    @Override
    public void handle(Node node) {
        whatsapp.disconnect(WhatsAppClientDisconnectReason.RECONNECTING);
    }
}
