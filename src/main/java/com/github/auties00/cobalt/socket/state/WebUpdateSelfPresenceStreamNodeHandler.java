package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

import java.time.ZonedDateTime;

public final class WebUpdateSelfPresenceStreamNodeHandler extends SocketStream.Handler {
    public WebUpdateSelfPresenceStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        var presence = new NodeBuilder()
                .description("presence")
                .attribute("name", whatsapp.store().name())
                .attribute("type", "available")
                .build();
        whatsapp.sendNodeWithNoResponse(presence);
        whatsapp.store().setOnline(true);
        whatsapp.store()
                .jid()
                .flatMap(whatsapp.store()::findContactByJid)
                .ifPresent(entry -> {
                    entry.setLastKnownPresence(ContactStatus.AVAILABLE);
                    entry.setLastSeen(ZonedDateTime.now());
                });
    }
}
