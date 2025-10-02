package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.io.node.Node;

public final class StreamEndNodeHandler extends AbstractNodeHandler {
    public StreamEndNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "xmlstreamend");
    }

    @Override
    public void handle(Node node) {
        whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
    }
}
