package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

final class StreamEndHandler extends NodeHandler.Dispatcher {
    StreamEndHandler(SocketConnection socketConnection) {
        super(socketConnection, "xmlstreamend");
    }

    @Override
    void execute(Node node) {
        socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
    }
}
