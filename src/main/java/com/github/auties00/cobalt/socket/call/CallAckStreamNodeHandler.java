package com.github.auties00.cobalt.socket.call;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.socket.SocketStream;

public final class CallAckStreamNodeHandler extends SocketStream.Handler {
    private static final byte[][] CALL_RELAY = new byte[][]{
            new byte[]{-105, 99, -47, -29, 13, -106},
            new byte[]{-99, -16, -53, 62, 13, -106},
            new byte[]{-99, -16, -25, 62, 13, -106},
            new byte[]{-99, -16, -5, 62, 13, -106},
            new byte[]{-71, 60, -37, 62, 13, -106}
    };

    public CallAckStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "ack");
    }

    @Override
    public void handle(Node node) {
        if(!node.hasAttribute("class", "call")) {
            return;
        }

        var relayNode = node.getChild("relay");
        if (relayNode.isEmpty()) {
            return;
        }

        var callCreator = relayNode.get()
                .getRequiredAttributeAsJid("call-creator");
        var callId = relayNode.get()
                .getRequiredAttributeAsString("call-id");
        relayNode.get()
                .streamChildren("participant")
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