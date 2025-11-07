package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebQueryBlockListStreamNodeHandler extends SocketStream.Handler {
    public WebQueryBlockListStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        for (var jid : whatsapp.queryBlockList()) {
            markBlocked(jid);
        }
    }

    private void markBlocked(Jid entry) {
        whatsapp.store()
                .findContactByJid(entry)
                .orElseGet(() -> {
                    var newContact = whatsapp.store().addNewContact(entry);
                    for(var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, newContact));
                    }
                    return newContact;
                })
                .setBlocked(true);
    }
}
