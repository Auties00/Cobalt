package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.call.CallAckStreamNodeHandler;
import com.github.auties00.cobalt.socket.call.CallStreamNodeHandler;
import com.github.auties00.cobalt.socket.error.ErrorStreamNodeHandler;
import com.github.auties00.cobalt.socket.error.FailureStreamNodeHandler;
import com.github.auties00.cobalt.socket.ib.IbStreamNodeHandler;
import com.github.auties00.cobalt.socket.iq.IqStreamNodeHandler;
import com.github.auties00.cobalt.socket.message.MessageAckStreamNodeHandler;
import com.github.auties00.cobalt.socket.message.MessageReceiptStreamNodeHandler;
import com.github.auties00.cobalt.socket.message.MessageStreamNodeHandler;
import com.github.auties00.cobalt.socket.notification.NotificationStreamNodeHandler;
import com.github.auties00.cobalt.socket.notification.PresenceStreamNodeHandler;
import com.github.auties00.cobalt.socket.state.EndStreamNodeHandler;
import com.github.auties00.cobalt.socket.state.SuccessStreamNodeHandler;

import java.util.*;

public final class SocketStream {
    private final Map<String, SequencedCollection<Handler>> handlers;
    public SocketStream(WhatsAppClient whatsapp, WhatsAppClientVerificationHandler.Web webVerificationHandler) {
        var pairingCode = webVerificationHandler instanceof WhatsAppClientVerificationHandler.Web.PairingCode
                ? new SocketPhonePairing()
                : null;
        this.handlers = withHandlers(
                new CallStreamNodeHandler(whatsapp),
                new CallAckStreamNodeHandler(whatsapp),
                new ErrorStreamNodeHandler(whatsapp),
                new FailureStreamNodeHandler(whatsapp),
                new IbStreamNodeHandler(whatsapp),
                new IqStreamNodeHandler(whatsapp, webVerificationHandler, pairingCode),
                new MessageStreamNodeHandler(whatsapp),
                new MessageAckStreamNodeHandler(whatsapp),
                new MessageReceiptStreamNodeHandler(whatsapp),
                new NotificationStreamNodeHandler(whatsapp, pairingCode),
                new PresenceStreamNodeHandler(whatsapp),
                new EndStreamNodeHandler(whatsapp),
                new SuccessStreamNodeHandler(whatsapp)
        );
    }

    private static Map<String, SequencedCollection<Handler>> withHandlers(Handler... handlers) {
        Map<String, SequencedCollection<Handler>> result = HashMap.newHashMap(handlers.length);
        for(var handler : handlers) {
            for(var description : handler.descriptions()) {
                result.compute(description, (_, values) -> {
                    if(values == null) {
                        values = new ArrayList<>();
                    }
                    values.add(handler);
                    return values;
                });
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public void digest(Node node) {
        var handlers = this.handlers.get(node.description());
        if(handlers != null) {
            for(var handler : handlers) {
                handler.handle(node);
            }
        }
    }

    public void dispose() {
        for (var entry : handlers.entrySet()) {
            for(var handler : entry.getValue()) {
                handler.dispose();
            }
        }
    }

    public abstract static class Handler {
        protected final WhatsAppClient whatsapp;
        protected final Set<String> descriptions;

        public Handler(WhatsAppClient whatsapp, String... descriptions) {
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
