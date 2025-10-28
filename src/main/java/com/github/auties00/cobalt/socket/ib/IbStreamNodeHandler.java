package com.github.auties00.cobalt.socket.ib;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.socket.SocketStream;

public final class IbStreamNodeHandler extends SocketStream.Handler {
    public IbStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "ib");
    }

    @Override
    public void handle(Node node) {
        var child = node.getChild();
        if(child.isEmpty()) {
            return;
        }

        switch(child.get().description()) {
            case "dirty" -> handleIbDirty(child.get());
            case "offline_preview" -> handleIbOfflinePreview(child.get());
        }
    }

    private void handleIbDirty(Node dirty) {
        var type = dirty.getRequiredAttributeAsString("type");
        // TODO: Support other types
        switch (type) {
            case "account_sync" -> handleAccountSync(dirty, type);
        }
    }

    private void handleAccountSync(Node dirty, String type) {
        var timestamp = dirty.getRequiredAttributeAsLong("timestamp");
        var queryBody = new NodeBuilder()
                .description("clean")
                .attribute("type", type)
                .attribute("timestamp", timestamp)
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .attribute("xmlns", "urn:xmpp:whatsapp:dirty")
                .content(queryBody);
        whatsapp.sendNode(queryRequest);
    }

    private void handleIbOfflinePreview(Node offlinePreview) {
        var count = offlinePreview.getAttributeAsLong("count", 0);
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
