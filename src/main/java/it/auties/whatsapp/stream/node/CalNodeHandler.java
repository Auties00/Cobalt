package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.model.call.CallBuilder;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.util.Clock;

public final class CalNodeHandler extends AbstractNodeHandler {
    public CalNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "call");
    }

    @Override
    void handle(Node node) {
        whatsapp.sendAck(node);
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
        whatsapp.store().addCall(call);
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onCall(call));
            Thread.startVirtualThread(() -> listener.onCall(whatsapp, call));
        }
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
