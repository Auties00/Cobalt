package it.auties.whatsapp.socket.stream.handler;

import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

public final class StreamEndNodeHandler extends AbstractNodeHandler {
    public StreamEndNodeHandler(SocketConnection socketConnection) {
        super(socketConnection, "xmlstreamend");
    }

    @Override
    public void handle(Node node) {
        socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
    }
}
