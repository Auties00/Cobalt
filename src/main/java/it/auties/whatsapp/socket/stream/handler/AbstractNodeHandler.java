package it.auties.whatsapp.socket.stream.handler;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

import java.util.Set;

public abstract class AbstractNodeHandler {
    protected final SocketConnection socketConnection;
    private final Set<String> descriptions;

    protected AbstractNodeHandler(SocketConnection socketConnection, String... descriptions) {
        this.socketConnection = socketConnection;
        this.descriptions = Set.of(descriptions);
    }

    public abstract void handle(Node node);

    public Set<String> descriptions() {
        return descriptions;
    }

    public void dispose() {

    }
}
