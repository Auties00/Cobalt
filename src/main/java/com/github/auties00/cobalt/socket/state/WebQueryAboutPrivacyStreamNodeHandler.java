package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebQueryAboutPrivacyStreamNodeHandler extends SocketStream.Handler {
    public WebQueryAboutPrivacyStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        var queryRequestBody = new NodeBuilder()
                .description("privacy")
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "status")
                .content(queryRequestBody);
        whatsapp.sendNode(queryRequest);
    }
}
