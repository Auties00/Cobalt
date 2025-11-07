package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebQueryGroupsStreamNodeHandler extends SocketStream.Handler {
    public WebQueryGroupsStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        if (whatsapp.store().syncedChats()) {
            return;
        }
        var _ = whatsapp.queryGroups();
    }
}
