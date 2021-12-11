package it.auties.whatsapp.protobuf.chat;

import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.exchange.Node;
import lombok.NonNull;

import java.util.NoSuchElementException;

/**
 * A model class that represents a participant of a group.
 *
 * @param id   the non-null id of the participant
 * @param role non-null role of the participant
 */
public record GroupParticipant(@NonNull ContactJid id, @NonNull GroupRole role) {
    /**
     * Constructs a new GroupParticipant from an input node
     *
     * @param node the non-null input node
     * @return a non-null GroupParticipant
     */
    public static GroupParticipant of(@NonNull Node node) {
        var id = node.attributes().getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing participant in group response"));
        var role = GroupRole.forData(node.attributes().getString("type", null));
        return new GroupParticipant(id, role);
    }
}
