package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;

import java.util.NoSuchElementException;
import java.util.Objects;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a participant of a group.
 *
 * @param jid  the non-null jid of the participant
 * @param role non-null role of the participant
 */
public record GroupParticipant(
        @ProtobufProperty(index = 1, type = STRING, name = "userJid", implementation = ContactJid.class) @NonNull ContactJid jid,
        @ProtobufProperty(index = 2, type = MESSAGE, name = "rank") GroupRole role)
        implements ProtobufMessage {

    public GroupParticipant(ContactJid jid, GroupRole role) {
        this.jid = jid;
        this.role = Objects.requireNonNullElse(role, GroupRole.USER);
    }

    /**
     * Constructs a new GroupParticipant from an input node
     *
     * @param node the non-null input node
     * @return a non-null GroupParticipant
     */
    public static GroupParticipant of(@NonNull Node node) {
        var id = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing participant in group response"));
        var role = GroupRole.of(node.attributes()
                                        .getString("type", null));
        return new GroupParticipant(id, role);
    }
}
