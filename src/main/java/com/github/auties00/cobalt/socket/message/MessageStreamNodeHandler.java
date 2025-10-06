package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.socket.SocketStream;

public final class MessageStreamNodeHandler extends SocketStream.Handler {
    private final MessageDeserializerHandler messageDeserializerHandler;

    public MessageStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "message");
        this.messageDeserializerHandler = new MessageDeserializerHandler(whatsapp);
    }

    @Override
    public void handle(Node node) {
        messageDeserializerHandler.decode(node, true);
    }
}
