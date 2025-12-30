package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.message.MessageReceiverService;
import com.github.auties00.cobalt.migration.LidMigrationService;
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
import com.github.auties00.cobalt.socket.state.*;

import java.util.*;

public final class SocketStream {
    private final Map<String, SequencedCollection<Handler>> handlers;

    public SocketStream(WhatsAppClient whatsapp, DeviceService deviceService, MessageReceiverService messageReceiverService, LidMigrationService lidMigrationService, WhatsAppClientVerificationHandler.Web webVerificationHandler) {
        var pairingCode = switch (webVerificationHandler) {
            case WhatsAppClientVerificationHandler.Web.PairingCode _ -> new SocketPhonePairing();
            case WhatsAppClientVerificationHandler.Web.QrCode _ -> null;
        };

        var result = new HashMap<String, SequencedCollection<Handler>>();

        // Common handlers
        addHandler(result, new CallStreamNodeHandler(whatsapp));
        addHandler(result, new CallAckStreamNodeHandler(whatsapp));
        addHandler(result, new ErrorStreamNodeHandler(whatsapp));
        addHandler(result, new FailureStreamNodeHandler(whatsapp));
        addHandler(result, new IbStreamNodeHandler(whatsapp));
        addHandler(result, new IqStreamNodeHandler(whatsapp, webVerificationHandler, pairingCode));
        addHandler(result, new MessageStreamNodeHandler(whatsapp, messageReceiverService, lidMigrationService));
        addHandler(result, new MessageAckStreamNodeHandler(whatsapp));
        addHandler(result, new MessageReceiptStreamNodeHandler(whatsapp, deviceService));
        addHandler(result, new NotificationStreamNodeHandler(whatsapp, pairingCode, lidMigrationService));
        addHandler(result, new PresenceStreamNodeHandler(whatsapp));
        addHandler(result, new EndStreamNodeHandler(whatsapp));
        addHandler(result, new UpdateIdentityStreamNodeHandler(whatsapp));

        // Session-specific handlers
        switch (whatsapp.store().clientType()) {
            case WEB -> {
                addHandler(result, new WebNotifyStoreStreamNodeHandler(whatsapp));
                addHandler(result, new WebQueryGroupsStreamNodeHandler(whatsapp));
                addHandler(result, new WebPullInitialAppStatePatchesStreamNodeHandler(whatsapp));
                addHandler(result, new WebSetActiveConnectionStreamNodeHandler(whatsapp));
                addHandler(result, new WebScheduleMediaConnectionUpdateStreamNodeHandler(whatsapp));
                addHandler(result, new WebUpdateSelfPresenceStreamNodeHandler(whatsapp));
                addHandler(result, new WebQuery2faStreamNodeHandler(whatsapp));
                addHandler(result, new WebQueryAboutPrivacyStreamNodeHandler(whatsapp));
                addHandler(result, new WebQueryPrivacySettingsStreamNodeHandler(whatsapp));
                addHandler(result, new WebQueryDisappearingModeStreamNodeHandler(whatsapp));
                addHandler(result, new WebQueryBlockListStreamNodeHandler(whatsapp));
                addHandler(result, new WebOnInitialInfoStreamNodeHandler(whatsapp));
                addHandler(result, new WebQueryNewslettersStreamNodeHandler(whatsapp));
            }
            case MOBILE -> {
                addHandler(result, new MobileFinishLoginStreamNodeHandler(whatsapp));
            }
        }

        this.handlers = Collections.unmodifiableMap(result);
    }

    private void addHandler(Map<String, SequencedCollection<Handler>> result, Handler handler) {
        for (var description : handler.descriptions()) {
            result.computeIfAbsent(description, _ -> new ArrayList<>()).add(handler);
        }
    }
    
    public void digest(Node node) {
        var handlers = this.handlers.get(node.description());
        if(handlers != null) {
            for(var handler : handlers) {
                Thread.startVirtualThread(() -> handler.handle(node));
            }
        }
    }

    public void reset() {
        for (var entry : handlers.entrySet()) {
            for(var handler : entry.getValue()) {
                handler.reset();
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

        public void reset() {

        }
    }
}
