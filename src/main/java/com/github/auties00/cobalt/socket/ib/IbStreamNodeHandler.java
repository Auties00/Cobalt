package com.github.auties00.cobalt.socket.ib;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.core.node.Node;
import com.github.auties00.cobalt.io.core.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

public final class IbStreamNodeHandler extends SocketStream.Handler {
    public IbStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "ib");
    }

    @Override
    public void handle(Node node) {
        var child = node.findChild();
        if(child.isEmpty()) {
            return;
        }

        switch(child.get().description()) {
            case "dirty" -> handleIbDirty(child.get());
            case "offline_preview" -> handleIbOfflinePreview(child.get());
        }
    }

    private void handleIbDirty(Node dirty) {
        var type = dirty.getRequiredAttribute("type")
                .toString();
        if (!type.equals("account_sync")) {
            return;
        }
        var timestamp = dirty.getRequiredAttribute("timestamp")
                .toString();
        var queryBody = new NodeBuilder()
                .description("clean")
                .attribute("type", type)
                .attribute("timestamp", timestamp)
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("type", "set")
                .attribute("xmlns", "urn:xmpp:whatsapp:dirty")
                .content(queryBody)
                .build();
        whatsapp.sendNode(queryRequest);
    }

    private void handleIbOfflinePreview(Node offlinePreview) {
        var count = offlinePreview.getAttribute("count")
                .map(attribute -> Long.parseUnsignedLong(attribute.toString()))
                .orElse(0L);
        var ibBody = new NodeBuilder()
                .description("offline_batch")
                .attribute("count", count)
                .build();
        var ibRequest = new NodeBuilder()
                .description("ib")
                .content(ibBody)
                .build();
        whatsapp.sendNodeWithNoResponse(ibRequest);
    }
}
