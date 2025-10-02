package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.io.node.Node;

public final class FailureNodeHandler extends AbstractNodeHandler {
    public FailureNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "failure");
    }

    @Override
    void handle(Node node) {
        var reason = node.attributes().getInt("reason");
        switch (reason) {
            case 503, 403 -> whatsapp.disconnect(WhatsappDisconnectReason.BANNED);
            case 401, 405 -> whatsapp.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            default -> whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
        }
    }
}
