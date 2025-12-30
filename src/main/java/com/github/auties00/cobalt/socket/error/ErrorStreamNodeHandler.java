package com.github.auties00.cobalt.socket.error;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.exception.MalformedNodeException;
import com.github.auties00.cobalt.exception.SessionBadMacException;
import com.github.auties00.cobalt.exception.SessionConflictException;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.STREAM;

public final class ErrorStreamNodeHandler extends SocketStream.Handler {
    private final AtomicBoolean retriedConnection;

    public ErrorStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "stream:error");
        this.retriedConnection = new AtomicBoolean(false);
    }

    @Override
    public void handle(Node node) {
        if (node.hasChild("xml-not-well-formed")) {
            whatsapp.handleFailure(STREAM, new MalformedNodeException());
        } else if (node.hasChild("conflict")) {
            whatsapp.handleFailure(STREAM, new SessionConflictException());
        } else if (node.hasChild("bad-mac")) {
            whatsapp.handleFailure(STREAM, new SessionBadMacException());
        } else {
            var statusCode = node.getAttributeAsLong("code");
            if(statusCode.isEmpty()) {
                handleError(node);
            } else {
                switch (statusCode.getAsLong()) {
                    case 403L, 503L -> handleBan();
                    case 500L -> handleLogout();
                    case 401L -> handleConflict(node);
                    default -> handleReconnect();
                }
            }
        }
    }

    private void handleReconnect() {
        whatsapp.disconnect(WhatsAppClientDisconnectReason.RECONNECTING);
    }

    private void handleBan() {
        var reason = retriedConnection.getAndSet(true)
                ? WhatsAppClientDisconnectReason.BANNED
                : WhatsAppClientDisconnectReason.RECONNECTING;
        whatsapp.disconnect(reason);
    }

    private void handleLogout() {
        whatsapp.disconnect(WhatsAppClientDisconnectReason.LOGGED_OUT);
    }

    private void handleConflict(Node node) {
        var type = node.getChild()
                .map(child -> child.getRequiredAttributeAsString("type"))
                .orElse("");
        var reason = node.getChild()
                .map(child -> child.getRequiredAttributeAsString("reason"))
                .orElse(type);
        if (reason.equals("device_removed")) {
            handleLogout();
        } else {
            whatsapp.handleFailure(STREAM, new SessionConflictException());
        }
    }

    private void handleError(Node node) {
        for (var error : node.children()) {
            whatsapp.resolvePendingRequest(error);
        }
    }

    @Override
    public void reset() {
        retriedConnection.set(false);
    }
}
