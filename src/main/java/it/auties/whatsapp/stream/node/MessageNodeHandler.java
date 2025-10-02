package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.stream.message.MessageDeserializerHandler;

public final class MessageNodeHandler extends AbstractNodeHandler {
    private final MessageDeserializerHandler messageDeserializerHandler;

    public MessageNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "message");
        this.messageDeserializerHandler = new MessageDeserializerHandler(whatsapp);
    }

    @Override
    public void handle(Node node) {
        messageDeserializerHandler.decode(node, true);
    }
}
