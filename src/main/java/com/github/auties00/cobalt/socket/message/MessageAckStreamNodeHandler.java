package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.core.node.Node;
import com.github.auties00.cobalt.model.proto.message.model.MessageStatus;
import com.github.auties00.cobalt.socket.SocketStream;

public final class MessageAckStreamNodeHandler extends SocketStream.Handler {
    public MessageAckStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "ack");
    }

    @Override
    public void handle(Node node) {
        if(!node.hasAttribute("class", "message")) {
            return;
        }

        var messageId = node.getRequiredAttributeAsString("id");
        var from = node.getRequiredAttributeAsJid("from");
        var match = whatsapp.store()
                .findMessageById(from, messageId);
        if (match.isEmpty()) {
            return;
        }

        var error = node.getAttributeAsLong("error");
        if (error.isPresent()) {
            match.get()
                    .setStatus(MessageStatus.ERROR);
        }else if (match.get().status() == MessageStatus.UNKNOWN || match.get().status().ordinal() < MessageStatus.SERVER_ACK.ordinal()) {
            match.get()
                    .setStatus(MessageStatus.SERVER_ACK);
        }
    }
}