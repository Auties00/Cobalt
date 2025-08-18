package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

final class MessageHandler extends NodeHandler.Executor {
    MessageHandler(SocketConnection socketConnection) {
        super(socketConnection, "message");
    }

    @Override
    void execute(Node node) {
        socketConnection.decodeMessage(node, null, true);
    }
}
