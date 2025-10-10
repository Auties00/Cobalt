package com.github.auties00.cobalt.socket.error;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappDisconnectReason;
import com.github.auties00.cobalt.exception.MalformedNodeException;
import com.github.auties00.cobalt.exception.SessionBadMacException;
import com.github.auties00.cobalt.exception.SessionConflictException;
import com.github.auties00.cobalt.io.core.node.Node;
import com.github.auties00.cobalt.io.core.node.NodeAttribute;
import com.github.auties00.cobalt.socket.SocketStream;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.auties00.cobalt.api.WhatsappErrorHandler.Location.STREAM;

public final class ErrorStreamNodeHandler extends SocketStream.Handler {
    private final AtomicBoolean retriedConnection;

    public ErrorStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "stream:error");
        this.retriedConnection = new AtomicBoolean(false);
    }

    @Override
    public void handle(Node node) {
        if (node.findChild("xml-not-well-formed").isPresent()) {
            whatsapp.handleFailure(STREAM, new MalformedNodeException());
            return;
        }

        if (node.findChild("conflict").isPresent()) {
            whatsapp.handleFailure(STREAM, new SessionConflictException());
            return;
        }

        if (node.findChild("bad-mac").isPresent()) {
            whatsapp.handleFailure(STREAM, new SessionBadMacException());
            return;
        }

        var statusCode = node.getRequiredAttribute("code")
                .toString();
        switch (statusCode) {
            case "403", "503" ->
                    whatsapp.disconnect(retriedConnection.getAndSet(true) ? WhatsappDisconnectReason.BANNED : WhatsappDisconnectReason.RECONNECTING);
            case "500" -> whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            case "401" -> {
                var type = node.findChild()
                        .map(child -> child.getRequiredAttribute("type"))
                        .map(NodeAttribute::toString)
                        .orElse("");
                var reason = node.findChild()
                        .map(child -> child.getRequiredAttribute("reason"))
                        .map(NodeAttribute::toString)
                        .orElse(type);
                if (!reason.equals("device_removed")) {
                    whatsapp.handleFailure(STREAM, new SessionConflictException());
                } else {
                    whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
                }
            }
            case "515" -> whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
            default -> node.children()
                    .forEach(whatsapp::resolvePendingRequest);
        }
    }

    @Override
    public void dispose() {
        retriedConnection.set(false);
    }
}
