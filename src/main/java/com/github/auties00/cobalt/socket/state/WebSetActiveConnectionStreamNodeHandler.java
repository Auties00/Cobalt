package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebSetActiveConnectionStreamNodeHandler extends SocketStream.Handler {
    public WebSetActiveConnectionStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        var active = new NodeBuilder()
                .description("active")
                .build();
        var query = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .attribute("xmlns", "passive")
                .content(active);
        whatsapp.sendNode(query);
    }
}
