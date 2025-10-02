package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.io.node.Node;

import java.util.Map;
import java.util.Objects;

public final class IbNodeHandler extends AbstractNodeHandler {
    public IbNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "ib");
    }

    @Override
    void handle(Node node) {
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
        var type = dirty.attributes().getString("type");
        if (!Objects.equals(type, "account_sync")) {
            return;
        }
        var timestamp = dirty.attributes().getString("timestamp");
        whatsapp.sendQuery("set", "urn:xmpp:whatsapp:dirty",
                Node.of("clean", Map.of("type", type, "timestamp", timestamp)));
    }

    private void handleIbOfflinePreview(Node offlinePreview) {
        var count = offlinePreview.attributes()
                .getLong("count");
        whatsapp.sendNodeWithNoResponse(Node.of("ib", Node.of("offline_batch", Map.of("count", count))));
    }
}
