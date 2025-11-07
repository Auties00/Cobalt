package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class MobileFinishLoginStreamNodeHandler extends SocketStream.Handler {
    public MobileFinishLoginStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        // TODO: Implement mobile login
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
