package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

import java.util.Map;
import java.util.Objects;

final class IbHandler extends NodeHandler.Dispatcher {
    IbHandler(SocketConnection socketConnection) {
        super(socketConnection, "ib");
    }

    @Override
    void execute(Node node) {
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
        socketConnection.sendQuery("set", "urn:xmpp:whatsapp:dirty",
                Node.of("clean", Map.of("type", type, "timestamp", timestamp)));
    }

    private void handleIbOfflinePreview(Node offlinePreview) {
        var count = offlinePreview.attributes()
                .getLong("count");
        socketConnection.sendNodeWithNoResponse(Node.of("ib", Node.of("offline_batch", Map.of("count", count))));
    }
}
