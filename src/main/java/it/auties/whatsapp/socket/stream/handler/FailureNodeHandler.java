package it.auties.whatsapp.socket.stream.handler;

import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

public final class FailureNodeHandler extends AbstractNodeHandler {
    public FailureNodeHandler(SocketConnection socketConnection) {
        super(socketConnection, "failure");
    }

    @Override
    public void handle(Node node) {
        var reason = node.attributes().getInt("reason");
        switch (reason) {
            case 503, 403 -> socketConnection.disconnect(WhatsappDisconnectReason.BANNED);
            case 401, 405 -> socketConnection.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            default -> socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
        }
    }
}
