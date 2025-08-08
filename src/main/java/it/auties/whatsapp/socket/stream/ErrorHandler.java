package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.exception.MalformedNodeException;
import it.auties.whatsapp.exception.SessionBadMacException;
import it.auties.whatsapp.exception.SessionConflictException;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.STREAM;

final class ErrorHandler extends NodeHandler.Dispatcher {
    private final AtomicBoolean retriedConnection;

    ErrorHandler(SocketConnection socketConnection) {
        super(socketConnection, "error");
        this.retriedConnection = new AtomicBoolean(false);
    }

    @Override
    void execute(Node node) {
        if (node.hasNode("xml-not-well-formed")) {
            socketConnection.handleFailure(STREAM, new MalformedNodeException());
            return;
        }

        if (node.hasNode("conflict")) {
            socketConnection.handleFailure(STREAM, new SessionConflictException());
            return;
        }

        if (node.hasNode("bad-mac")) {
            socketConnection.handleFailure(STREAM, new SessionBadMacException());
            return;
        }

        var statusCode = node.attributes().getInt("code");
        switch (statusCode) {
            case 403, 503 ->
                    socketConnection.disconnect(retriedConnection.getAndSet(true) ? WhatsappDisconnectReason.BANNED : WhatsappDisconnectReason.RECONNECTING);
            case 500 -> socketConnection.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            case 401 -> {
                var child = node.children().getFirst();
                var type = child.attributes().getString("type");
                var reason = child.attributes().getString("reason", type);
                if (!Objects.equals(reason, "device_removed")) {
                    socketConnection.handleFailure(STREAM, new RuntimeException(reason));
                } else {
                    socketConnection.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
                }
            }
            case 515 -> socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
            default -> node.children()
                    .forEach(socketConnection::resolvePendingRequest);
        }
    }

    @Override
    void dispose() {
        retriedConnection.set(false);
    }
}
