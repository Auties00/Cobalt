package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.socket.stream.handler.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StreamComponent {
    private final ExecutorService executor;
    private final Map<String, AbstractNodeHandler> handlers;
    public StreamComponent(SocketConnection socketConnection, WhatsappVerificationHandler.Web webVerificationHandler) {
        this.executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        var pairingCode = new PairingCodeComponent();
        this.handlers = withHandlers(
                new AckNodeHandler(socketConnection),
                new CalNodelHandler(socketConnection),
                new ErrorNodeHandler(socketConnection),
                new FailureNodeHandler(socketConnection),
                new IbNodeHandler(socketConnection),
                new IqNodeHandler(socketConnection, webVerificationHandler, pairingCode),
                new MessageNodeHandler(socketConnection),
                new NotificationNodeHandler(socketConnection, pairingCode),
                new ReceiptNodeHandler(socketConnection),
                new StateNodeHandler(socketConnection),
                new StreamEndNodeHandler(socketConnection),
                new SuccessNodeHandler(socketConnection)
        );
    }

    private static Map<String, AbstractNodeHandler> withHandlers(AbstractNodeHandler... handlers) {
        Map<String, AbstractNodeHandler> result = HashMap.newHashMap(handlers.length);
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

    public void digest(Node node) {
        executor.execute(() -> {
            var handler = handlers.get(node.description());
            if(handler != null) {
                handler.handle(node);
            }
        });
    }

    public void dispose() {
        executor.shutdownNow();
        handlers.forEach((_, handler) -> handler.dispose());
    }

}
