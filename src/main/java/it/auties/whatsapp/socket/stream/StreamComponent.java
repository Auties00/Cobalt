package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StreamComponent {
    private final Map<String, NodeHandler> handlers;
    public StreamComponent(SocketConnection socketConnection) {
        this.handlers = withHandlers(
                new AckHandler(socketConnection),
                new CallHandler(socketConnection),
                new ErrorHandler(socketConnection),
                new FailureHandler(socketConnection),
                new IbHandler(socketConnection),
                new IqHandler(socketConnection),
                new MessageHandler(socketConnection),
                new NotificationHandler(socketConnection),
                new ReceiptHandler(socketConnection),
                new StateHandler(socketConnection),
                new StreamEndHandler(socketConnection),
                new SuccessHandler(socketConnection)
        );
    }

    private static Map<String, NodeHandler> withHandlers(NodeHandler... handlers) {
        Map<String, NodeHandler> result = HashMap.newHashMap(handlers.length);
        for(var handler : handlers) {
            for(var description : handler.descriptions()) {
                var existing = result.put(description, handler);
                if(existing != null) {
                    throw new IllegalStateException("A handler for the description " + description + " is already present");
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public void dispose() {
        handlers.forEach((key, handler) -> handler.dispose());
    }

    public void digest(Node node) {
        var handler = handlers.get(node.description());
        if(handler != null) {
            handler.handle(node);
        }
    }
}
