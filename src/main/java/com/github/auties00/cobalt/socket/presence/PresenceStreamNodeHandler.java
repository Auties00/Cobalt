package com.github.auties00.cobalt.socket.presence;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.io.node.NodeAttribute;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.socket.SocketStream;

import java.time.ZonedDateTime;

public final class PresenceStreamNodeHandler extends SocketStream.Handler {
    public PresenceStreamNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "presence", "chatstate");
    }

    @Override
    public void handle(Node node) {
        var status = getUpdateType(node);
        var chatJid = node.getRequiredAttribute("from")
                .toJid();
        var participantJid = node.getOptionalAttribute("participant")
                .map(NodeAttribute::toJid);
        if(participantJid.isEmpty()) {
            whatsapp.store()
                    .findContactByJid(chatJid)
                    .ifPresent(contact -> {
                        contact.setLastKnownPresence(status);
                        contact.setLastSeen(ZonedDateTime.now());
                    });
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onContactPresence(chatJid, chatJid));
                Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, chatJid, chatJid));
            }
        } else {
            whatsapp.store()
                    .findContactByJid(chatJid)
                    .ifPresent(contact -> {
                        contact.setLastKnownPresence(ContactStatus.AVAILABLE);
                        contact.setLastSeen(ZonedDateTime.now());
                    });
            whatsapp.store()
                    .findChatByJid(chatJid)
                    .ifPresent(chat -> chat.addPresence(participantJid.get(), status));
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onContactPresence(chatJid, participantJid.get()));
                Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, chatJid, participantJid.get()));
            }
        }
    }
    private ContactStatus getUpdateType(Node node) {
        var media = node.firstChild()
                .flatMap(entry -> entry.getOptionalAttribute("media"))
                .map(NodeAttribute::toString)
                .orElse("");
        if (media.equals("audio")) {
            return ContactStatus.RECORDING;
        }

        return node.getOptionalAttribute("type")
                .map(NodeAttribute::toString)
                .or(() -> node.firstChild().map(Node::description))
                .flatMap(ContactStatus::of)
                .orElse(ContactStatus.AVAILABLE);
    }
}
