package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappVerificationHandler;
import com.github.auties00.cobalt.core.node.Node;
import com.github.auties00.cobalt.socket.ack.AckStreamNodeHandler;
import com.github.auties00.cobalt.socket.call.CallStreamNodeHandler;
import com.github.auties00.cobalt.socket.error.ErrorStreamNodeHandler;
import com.github.auties00.cobalt.socket.error.FailureStreamNodeHandler;
import com.github.auties00.cobalt.socket.ib.IbStreamNodeHandler;
import com.github.auties00.cobalt.socket.iq.IqStreamNodeHandler;
import com.github.auties00.cobalt.socket.message.MessageStreamNodeHandler;
import com.github.auties00.cobalt.socket.notification.NotificationStreamNodeHandler;
import com.github.auties00.cobalt.socket.presence.PresenceStreamNodeHandler;
import com.github.auties00.cobalt.socket.receipt.ReceiptStreamNodeHandler;
import com.github.auties00.cobalt.socket.state.EndStreamNodeHandler;
import com.github.auties00.cobalt.socket.state.SuccessStreamNodeHandler;
import com.github.auties00.cobalt.util.PhonePairingCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SocketStream {
    private final Map<String, Handler> handlers;
    public SocketStream(Whatsapp whatsapp, WhatsappVerificationHandler.Web webVerificationHandler) {
        var pairingCode = new PhonePairingCode();
        this.handlers = withHandlers(
                new AckStreamNodeHandler(whatsapp),
                new CallStreamNodeHandler(whatsapp),
                new ErrorStreamNodeHandler(whatsapp),
                new FailureStreamNodeHandler(whatsapp),
                new IbStreamNodeHandler(whatsapp),
                new IqStreamNodeHandler(whatsapp, webVerificationHandler, pairingCode),
                new MessageStreamNodeHandler(whatsapp),
                new NotificationStreamNodeHandler(whatsapp, pairingCode),
                new ReceiptStreamNodeHandler(whatsapp),
                new PresenceStreamNodeHandler(whatsapp),
                new EndStreamNodeHandler(whatsapp),
                new SuccessStreamNodeHandler(whatsapp)
        );
    }

    private static Map<String, Handler> withHandlers(Handler... handlers) {
        Map<String, Handler> result = HashMap.newHashMap(handlers.length);
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
        var handler = handlers.get(node.description());
        if(handler != null) {
            handler.handle(node);
        }
    }

    public void dispose() {
        for (var entry : handlers.entrySet()) {
            var handler = entry.getValue();
            handler.dispose();
        }
    }

    public abstract static class Handler {
        protected final Whatsapp whatsapp;
        protected final Set<String> descriptions;

        public Handler(Whatsapp whatsapp, String... descriptions) {
            this.whatsapp = whatsapp;
            this.descriptions = Set.of(descriptions);
        }

        public abstract void handle(Node node);

        public Set<String> descriptions() {
            return descriptions;
        }

        public void dispose() {

        }
    }
}
