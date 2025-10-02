package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.exception.MalformedNodeException;
import it.auties.whatsapp.exception.SessionBadMacException;
import it.auties.whatsapp.exception.SessionConflictException;
import it.auties.whatsapp.io.node.Node;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.STREAM;

public final class ErrorNodeHandler extends AbstractNodeHandler {
    private final AtomicBoolean retriedConnection;

    public ErrorNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "stream:error");
        this.retriedConnection = new AtomicBoolean(false);
    }

    @Override
    void handle(Node node) {
        if (node.hasNode("xml-not-well-formed")) {
            whatsapp.handleFailure(STREAM, new MalformedNodeException());
            return;
        }

        if (node.hasNode("conflict")) {
            whatsapp.handleFailure(STREAM, new SessionConflictException());
            return;
        }

        if (node.hasNode("bad-mac")) {
            whatsapp.handleFailure(STREAM, new SessionBadMacException());
            return;
        }

        var statusCode = node.attributes().getInt("code");
        switch (statusCode) {
            case 403, 503 ->
                    whatsapp.disconnect(retriedConnection.getAndSet(true) ? WhatsappDisconnectReason.BANNED : WhatsappDisconnectReason.RECONNECTING);
            case 500 -> whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            case 401 -> {
                var child = node.children().getFirst();
                var type = child.attributes().getString("type");
                var reason = child.attributes().getString("reason", type);
                if (!Objects.equals(reason, "device_removed")) {
                    whatsapp.handleFailure(STREAM, new SessionConflictException());
                } else {
                    whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
                }
            }
            case 515 -> whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
            default -> node.children()
                    .forEach(whatsapp::resolvePendingRequest);
        }
    }

    @Override
    void dispose() {
        retriedConnection.set(false);
    }
}
