package com.github.auties00.cobalt.socket.ack;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.io.node.NodeAttribute;
import com.github.auties00.cobalt.io.node.NodeBuilder;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.socket.SocketStream;

import java.nio.ByteBuffer;

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
        var ackClass = node.getRequiredAttribute("class")
                .toString();
        switch (ackClass) {
            case "call" -> digestCallAck(node);
            case "message" -> digestMessageAck(node);
        }
    }

    private void digestMessageAck(Node node) {
        var error = node.getOptionalAttribute("error")
                .isPresent();
        var messageId = node.getRequiredAttribute("id")
                .toString();
        var from = node.getRequiredAttribute("from")
                .toJid();
        var match = whatsapp.store()
                .findMessageById(from, messageId)
                .orElse(null);
        if (match == null) {
            return;
        }

        if (error) {
            match.setStatus(MessageStatus.ERROR);
        }else if (match.status() == MessageStatus.UNKNOWN || match.status().ordinal() < MessageStatus.SERVER_ACK.ordinal()) {
            match.setStatus(MessageStatus.SERVER_ACK);
        }
    }

    private void digestCallAck(Node node) {
        var relayNode = node.firstChildByDescription("relay").orElse(null);
        if (relayNode == null) {
            return;
        }

        var callCreator = relayNode.getRequiredAttribute("call-creator")
                .toJid();
        var callId = relayNode.getRequiredAttribute("call-id")
                .toString();
        relayNode.streamChildrenByDescription("participant")
                .map(entry -> entry.getRequiredAttribute("value"))
                .map(NodeAttribute::toJid)
                .forEach(to -> sendRelay(callCreator, callId, to));
    }

    private void sendRelay(Jid callCreator, String callId, Jid to) {
        for (var value : CALL_RELAY) {
            var te = new NodeBuilder()
                    .description("te")
                    .attribute("latency", 33554440)
                    .content(ByteBuffer.wrap(value))
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
                    .content(relay)
                    .build();
            whatsapp.sendNode(call);
        }
    }
}
