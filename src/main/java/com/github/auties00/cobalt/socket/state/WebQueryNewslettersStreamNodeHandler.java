package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebQueryNewslettersStreamNodeHandler extends SocketStream.Handler {
    public WebQueryNewslettersStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        if (whatsapp.store().syncedNewsletters()) {
            return;
        }
        var newsletters = whatsapp.queryNewsletters();
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onNewsletters(whatsapp, newsletters));
        }
    }
}
