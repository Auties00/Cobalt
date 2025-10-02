package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.model.contact.ContactStatus;

public final class StateNodeHandler extends AbstractNodeHandler {
    public StateNodeHandler(Whatsapp whatsapp) {
        super(whatsapp, "presence", "chatstate");
    }

    @Override
    public void handle(Node node) {
        var chatJid = node.attributes()
                .getRequiredJid("from");
        var participantJid = node.attributes()
                .getOptionalJid("participant")
                .orElse(chatJid);
        ContactStatus status = getUpdateType(node);
        whatsapp.store()
                .findChatByJid(chatJid)
                .ifPresent(chat -> {
                    for(var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onContactPresence(chat, participantJid));
                        Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, chat, participantJid));
                    }
                });
    }
    private ContactStatus getUpdateType(Node node) {
        var metadata = node.findChild();
        var recording = metadata.map(entry -> entry.attributes().getString("media"))
                .filter(entry -> entry.equals("audio"))
                .isPresent();
        if (recording) {
            return ContactStatus.RECORDING;
        }

        return node.attributes()
                .getOptionalString("type")
                .or(() -> metadata.map(Node::description))
                .flatMap(ContactStatus::of)
                .orElse(ContactStatus.AVAILABLE);
    }
}
