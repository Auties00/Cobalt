package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

final class FailureHandler extends NodeHandler.Dispatcher {
    FailureHandler(SocketConnection socketConnection) {
        super(socketConnection, "failure");
    }

    @Override
    void execute(Node node) {
        var reason = node.attributes().getInt("reason");
        switch (reason) {
            case 503, 403 -> socketConnection.disconnect(WhatsappDisconnectReason.BANNED);
            case 401, 405 -> socketConnection.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            default -> socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
        }
    }
}
