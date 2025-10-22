package com.github.auties00.cobalt.socket.error;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappDisconnectReason;
import com.github.auties00.cobalt.exception.MalformedNodeException;
import com.github.auties00.cobalt.exception.SessionBadMacException;
import com.github.auties00.cobalt.exception.SessionConflictException;
import com.github.auties00.cobalt.io.core.node.Node;
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
        if (node.hasChild("xml-not-well-formed")) {
            whatsapp.handleFailure(STREAM, new MalformedNodeException());
            return;
        }

        if (node.hasChild("conflict")) {
            whatsapp.handleFailure(STREAM, new SessionConflictException());
            return;
        }

        if (node.hasChild("bad-mac")) {
            whatsapp.handleFailure(STREAM, new SessionBadMacException());
            return;
        }

        var statusCode = Math.toIntExact(node.getRequiredAttributeAsLong("code"));
        switch (statusCode) {
            case 403, 503 -> handleBan();
            case 500 -> handleLogout();
            case 401 -> handleConflict(node);
            case 515 -> handleReconnect();
            default -> handleError(node);
        }
    }

    private void handleReconnect() {
        whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
    }

    private void handleBan() {
        var reason = retriedConnection.getAndSet(true)
                ? WhatsappDisconnectReason.BANNED
                : WhatsappDisconnectReason.RECONNECTING;
        whatsapp.disconnect(reason);
    }

    private void handleLogout() {
        whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
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
    public void dispose() {
        retriedConnection.set(false);
    }
}
