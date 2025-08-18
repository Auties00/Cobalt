package it.auties.whatsapp.socket.stream;

import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.socket.SocketConnection;

class StateHandler extends NodeHandler.Dispatcher {
    StateHandler(SocketConnection socketConnection) {
        super(socketConnection, "presence", "chatstate");
    }

    @Override
    void execute(Node node) {
        var chatJid = node.attributes()
                .getRequiredJid("from");
        var participantJid = node.attributes()
                .getOptionalJid("participant")
                .orElse(chatJid);
        ContactStatus status = getUpdateType(node);
        socketConnection.store()
                .findChatByJid(chatJid)
                .ifPresent(chat -> socketConnection.onUpdateChatPresence(status, participantJid, chat));
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
