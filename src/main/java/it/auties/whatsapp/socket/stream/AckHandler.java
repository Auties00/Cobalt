package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

import java.util.Map;
import java.util.Optional;

final class AckHandler extends NodeHandler.Dispatcher {
    private static final byte[][] CALL_RELAY = new byte[][]{
            new byte[]{-105, 99, -47, -29, 13, -106},
            new byte[]{-99, -16, -53, 62, 13, -106},
            new byte[]{-99, -16, -25, 62, 13, -106},
            new byte[]{-99, -16, -5, 62, 13, -106},
            new byte[]{-71, 60, -37, 62, 13, -106}
    };

    AckHandler(SocketConnection socketConnection) {
        super(socketConnection, "ack");
    }

    @Override
    void execute(Node node) {
        var ackClass = node.attributes().getString("class");
        switch (ackClass) {
            case "call" -> digestCallAck(node);
            case "message" -> digestMessageAck(node);
        }
    }

    private void digestMessageAck(Node node) {
        var error = node.attributes().getInt("error");
        var messageId = node.id();
        var from = node.attributes()
                .getRequiredJid("from");
        var match = socketConnection.store()
                .findMessageById(from, messageId)
                .orElse(null);
        if (match == null) {
            return;
        }

        if (error != 0) {
            match.setStatus(MessageStatus.ERROR);
            return;
        }

        if (match.status().ordinal() >= MessageStatus.SERVER_ACK.ordinal()) {
            return;
        }

        match.setStatus(MessageStatus.SERVER_ACK);
    }

    private void digestCallAck(Node node) {
        var relayNode = node.findChild("relay").orElse(null);
        if (relayNode == null) {
            return;
        }

        var callCreator = relayNode.attributes()
                .getRequiredJid("call-creator");
        var callId = relayNode.attributes()
                .getString("call-id");
        relayNode.listChildren("participant")
                .stream()
                .map(entry -> entry.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .forEach(to -> sendRelay(callCreator, callId, to));
    }

    private void sendRelay(Jid callCreator, String callId, Jid to) {
        for (var value : CALL_RELAY) {
            var te = Node.of("te", Map.of("latency", 33554440), value);
            var relay = Node.of("relaylatency", Map.of("call-creator", callCreator, "call-id", callId), te);
            socketConnection.sendNode(Node.of("call", Map.of("to", to), relay));
        }
    }
}
