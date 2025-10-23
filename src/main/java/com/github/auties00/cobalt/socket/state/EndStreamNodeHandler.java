package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappDisconnectReason;
import com.github.auties00.cobalt.model.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class EndStreamNodeHandler extends SocketStream.Handler {
    public EndStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "xmlstreamend");
    }

    @Override
    public void handle(Node node) {
        whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
    }
}
