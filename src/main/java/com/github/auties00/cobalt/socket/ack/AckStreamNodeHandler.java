package com.github.auties00.cobalt.socket.ack;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.core.node.Node;
import com.github.auties00.cobalt.core.node.NodeBuilder;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.socket.SocketStream;

public final class AckStreamNodeHandler extends SocketStream.Handler {
    private static final byte[][] CALL_RELAY = new byte[][]{
            new byte[]{-105, 99, -47, -29, 13, -106},
            new byte[]{-99, -16, -53, 62, 13, -106},
            new byte[]{-99, -16, -25, 62, 13, -106},
            new byte[]{-99, -16, -5, 62, 13, -106},
            new byte[]{-71, 60, -37, 62, 13, -106}
    };

    public AckStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "ack");
    }

    @Override
    public void handle(Node node) {
        var ackClass = node.getRequiredAttributeAsString("class");
        switch (ackClass) {
            case "call" -> digestCallAck(node);
            case "message" -> digestMessageAck(node);
        }
    }

    private void digestMessageAck(Node node) {
        var messageId = node.getRequiredAttributeAsString("id");
        var from = node.getRequiredAttributeAsJid("from");
        var match = whatsapp.store()
                .findMessageById(from, messageId)
                .orElse(null);
        if (match == null) {
            return;
        }

        var error = node.getAttributeAsLong("error");
        if (error.isPresent()) {
            match.setStatus(MessageStatus.ERROR);
        }else if (match.status() == MessageStatus.UNKNOWN || match.status().ordinal() < MessageStatus.SERVER_ACK.ordinal()) {
            match.setStatus(MessageStatus.SERVER_ACK);
        }
    }

    private void digestCallAck(Node node) {
        var relayNode = node.getChild("relay").orElse(null);
        if (relayNode == null) {
            return;
        }

        var callCreator = relayNode.getRequiredAttributeAsJid("call-creator");
        var callId = relayNode.getRequiredAttributeAsString("call-id");
        relayNode.streamChildren("participant")
                .flatMap(entry -> entry.streamAttributeAsJid("jid"))
                .forEach(to -> sendRelay(callCreator, callId, to));
    }

    private void sendRelay(Jid callCreator, String callId, Jid to) {
        for (var value : CALL_RELAY) {
            var te = new NodeBuilder()
                    .description("te")
                    .attribute("latency", 33554440)
                    .content(value)
                    .build();
            var relay = new NodeBuilder()
                    .description("relaylatency")
                    .attribute("call-creator", callCreator)
                    .attribute("call-id", callId)
                    .content(te)
                    .build();
            var call = new NodeBuilder()
                    .description("call")
                    .attribute("to", to)
                    .content(relay);
            whatsapp.sendNode(call);
        }
    }
}