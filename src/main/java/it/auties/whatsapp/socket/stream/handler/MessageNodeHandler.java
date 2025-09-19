package it.auties.whatsapp.socket.stream.handler;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

public final class MessageNodeHandler extends AbstractNodeHandler {
    public MessageNodeHandler(SocketConnection socketConnection) {
        super(socketConnection, "message");
    }

    @Override
    public void handle(Node node) {
        socketConnection.decodeMessage(node, null, true);
    }
}
