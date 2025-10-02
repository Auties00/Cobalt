package it.auties.whatsapp.stream;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.stream.node.*;
import it.auties.whatsapp.util.PhonePairingCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StreamHandler {
    private final ExecutorService executor;
    private final Map<String, AbstractNodeHandler> handlers;
    public StreamHandler(Whatsapp whatsapp, WhatsappVerificationHandler.Web webVerificationHandler) {
        this.executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        var pairingCode = new PhonePairingCode();
        this.handlers = withHandlers(
                new AckNodeHandler(whatsapp),
                new CalNodeHandler(whatsapp),
                new ErrorNodeHandler(whatsapp),
                new FailureNodeHandler(whatsapp),
                new IbNodeHandler(whatsapp),
                new IqNodeHandler(whatsapp, webVerificationHandler, pairingCode),
                new MessageNodeHandler(whatsapp),
                new NotificationNodeHandler(whatsapp, pairingCode),
                new ReceiptNodeHandler(whatsapp),
                new StateNodeHandler(whatsapp),
                new StreamEndNodeHandler(whatsapp),
                new SuccessNodeHandler(whatsapp)
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
