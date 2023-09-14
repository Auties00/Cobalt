package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;

import java.util.Objects;

/**
 * A model class that represents a participant of a group.
 */
public final class GroupParticipant implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final ContactJid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
    private GroupRole role;

    public GroupParticipant(ContactJid jid, GroupRole role) {
        this.jid = jid;
        this.role = Objects.requireNonNullElse(role, GroupRole.USER);
    }

    public ContactJid jid() {
        return jid;
    }

    public GroupRole role() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }
}
