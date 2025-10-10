package com.github.auties00.cobalt.socket.call;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.core.node.Node;
import com.github.auties00.cobalt.io.core.node.NodeAttribute;
import com.github.auties00.cobalt.model.call.CallBuilder;
import com.github.auties00.cobalt.model.call.CallStatus;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.Clock;

public final class CalStreamNodeHandler extends SocketStream.Handler {
    public CalStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "call");
    }

    @Override
    public void handle(Node node) {
        whatsapp.sendAck(node);
        var callNode = node.findChild()
                .orElse(null);
        if (callNode == null) {
            return;
        }

        if (!callNode.hasDescription("offer")) {
            return;
        }

        var from = node.getRequiredAttribute("from")
                .toJid();
        var callId = callNode.getRequiredAttribute("call-id")
                .toString();
        var caller = callNode.getAttribute("call-creator")
                .map(NodeAttribute::toJid)
                .orElse(from);
        var status = getCallStatus(callNode);
        var timestampSeconds = callNode.getAttribute("t")
                .map(entry -> Long.parseUnsignedLong(entry.toString()))
                .orElseGet(Clock::nowSeconds);
        var isOffline = callNode.getAttribute("offline")
                .isPresent();
        var hasVideo = callNode.findChild("video")
                .isPresent();
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
            case "terminate" -> {
                var reason = node.getAttribute("reason")
                        .map(NodeAttribute::toString)
                        .orElse("");
                yield reason.equals("timeout") ? CallStatus.TIMED_OUT : CallStatus.REJECTED;
            }
            case "reject" -> CallStatus.REJECTED;
            case "accept" -> CallStatus.ACCEPTED;
            default -> CallStatus.RINGING;
        };
    }
}
