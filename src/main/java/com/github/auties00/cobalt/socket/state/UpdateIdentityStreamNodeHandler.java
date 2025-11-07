package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class UpdateIdentityStreamNodeHandler extends SocketStream.Handler {
    public UpdateIdentityStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        node.getAttributeAsJid("lid")
                .ifPresent(whatsapp.store()::setLid);
    }
}
