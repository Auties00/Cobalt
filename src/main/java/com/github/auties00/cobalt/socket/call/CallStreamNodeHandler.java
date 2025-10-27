package com.github.auties00.cobalt.socket.call;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.core.node.Node;
import com.github.auties00.cobalt.model.proto.call.CallBuilder;
import com.github.auties00.cobalt.model.proto.call.CallStatus;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.Clock;

public final class CallStreamNodeHandler extends SocketStream.Handler {
    public CallStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "call");
    }

    @Override
    public void handle(Node node) {
        whatsapp.sendAck(node);
        var callNode = node.getChild();
        if (callNode.isEmpty()) {
            return;
        }

        // TODO: Support other types
        switch (callNode.get().description()) {
            case "offer" -> handleOffer(node, callNode.get());
        }
    }

    private void handleOffer(Node infoNode, Node callNode) {
        var from = infoNode.getRequiredAttributeAsJid("from");
        var callId = callNode.getRequiredAttributeAsString("call-id");
        var caller = callNode.getAttributeAsJid("call-creator", from);
        var status = getCallStatus(callNode);
        var timestampSeconds = callNode.getAttributeAsLong("t")
                .orElseGet(Clock::nowSeconds);
        var isOffline = callNode.hasAttribute("offline");
        var hasVideo = callNode.hasChild("video");
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
            Thread.startVirtualThread(() -> listener.onCall(whatsapp, call));
        }
    }

    private CallStatus getCallStatus(Node node) {
        return switch (node.description()) {
            case "terminate" -> node.hasAttribute("reason", "timeout") ? CallStatus.TIMED_OUT : CallStatus.REJECTED;
            case "reject" -> CallStatus.REJECTED;
            case "accept" -> CallStatus.ACCEPTED;
            default -> CallStatus.RINGING;
        };
    }
}