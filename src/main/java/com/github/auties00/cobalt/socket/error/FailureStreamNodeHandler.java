package com.github.auties00.cobalt.socket.error;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class FailureStreamNodeHandler extends SocketStream.Handler {
    public FailureStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "failure");
    }

    @Override
    public void handle(Node node) {
        var reason = node.getRequiredAttributeAsLong("reason");
        switch (reason) {
            case 503L, 403L -> whatsapp.disconnect(WhatsAppClientDisconnectReason.BANNED);
            case 401L, 405L -> whatsapp.disconnect(WhatsAppClientDisconnectReason.LOGGED_OUT);
            default -> whatsapp.disconnect(WhatsAppClientDisconnectReason.RECONNECTING);
        }
    }
}
