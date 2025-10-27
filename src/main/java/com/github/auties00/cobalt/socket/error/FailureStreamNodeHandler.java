package com.github.auties00.cobalt.socket.error;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.model.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class FailureStreamNodeHandler extends SocketStream.Handler {
    public FailureStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "failure");
    }

    @Override
    public void handle(Node node) {
        var reason = Math.toIntExact(node.getRequiredAttributeAsLong("reason"));
        switch (reason) {
            case 503, 403 -> whatsapp.disconnect(WhatsAppClientDisconnectReason.BANNED);
            case 401, 405 -> whatsapp.disconnect(WhatsAppClientDisconnectReason.LOGGED_OUT);
            default -> whatsapp.disconnect(WhatsAppClientDisconnectReason.RECONNECTING);
        }
    }
}
