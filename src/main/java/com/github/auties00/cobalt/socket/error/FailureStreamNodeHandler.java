package com.github.auties00.cobalt.socket.error;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappDisconnectReason;
import com.github.auties00.cobalt.model.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class FailureStreamNodeHandler extends SocketStream.Handler {
    public FailureStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "failure");
    }

    @Override
    public void handle(Node node) {
        var reason = Math.toIntExact(node.getRequiredAttributeAsLong("reason"));
        switch (reason) {
            case 503, 403 -> whatsapp.disconnect(WhatsappDisconnectReason.BANNED);
            case 401, 405 -> whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            default -> whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
        }
    }
}
