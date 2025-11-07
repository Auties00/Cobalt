package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class WebPullInitialAppStatePatchesStreamNodeHandler extends SocketStream.Handler {
    public WebPullInitialAppStatePatchesStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        if (!whatsapp.store().hasPreKeys() || whatsapp.store().syncedWebAppState()) {
            return;
        }
        whatsapp.pullWebAppState(PatchType.values());
        whatsapp.store()
                .setSyncedWebAppState(true);
    }
}
