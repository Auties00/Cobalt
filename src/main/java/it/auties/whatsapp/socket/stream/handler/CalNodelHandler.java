package it.auties.whatsapp.socket.stream.handler;

import it.auties.whatsapp.model.call.CallBuilder;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.Clock;

public final class CalNodelHandler extends AbstractNodeHandler {
    public CalNodelHandler(SocketConnection socketConnection) {
        super(socketConnection, "call");
    }

    @Override
    public void handle(Node node) {
        socketConnection.sendAck(node);
        var callNode = node.children().peekFirst();
        if (callNode == null) {
            return;
        }

        if (!callNode.description().equals("offer")) {
            return;
        }

        var from = node.attributes()
                .getRequiredJid("from");
        var callId = callNode.attributes()
                .getString("call-id");
        var caller = callNode.attributes()
                .getOptionalJid("call-creator")
                .orElse(from);
        var status = getCallStatus(callNode);
        var timestampSeconds = callNode.attributes()
                .getOptionalLong("t")
                .orElseGet(Clock::nowSeconds);
        var isOffline = callNode.attributes().hasKey("offline");
        var hasVideo = callNode.hasNode("video");
        var call = new CallBuilder()
                .chatJid(from)
                .callerJid(caller)
                .id(callId)
                .timestampSeconds(timestampSeconds)
                .video(hasVideo)
                .status(status)
                .offline(isOffline)
                .build();
        socketConnection.store().addCall(call);
        socketConnection.onCall(call);
    }

    private CallStatus getCallStatus(Node node) {
        return switch (node.description()) {
            case "terminate" ->
                    node.attributes().hasValue("reason", "timeout") ? CallStatus.TIMED_OUT : CallStatus.REJECTED;
            case "reject" -> CallStatus.REJECTED;
            case "accept" -> CallStatus.ACCEPTED;
            default -> CallStatus.RINGING;
        };
    }
}
