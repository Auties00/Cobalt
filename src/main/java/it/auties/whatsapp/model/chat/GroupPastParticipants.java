package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.List;
import java.util.Objects;

/**
 * Class representing a list of past participants in a chat group
 */
@ProtobufMessage(name = "PastParticipants")
public final class GroupPastParticipants {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid groupJid;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<ChatPastParticipant> pastParticipants;

    GroupPastParticipants(Jid groupJid, List<ChatPastParticipant> pastParticipants) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.pastParticipants = Objects.requireNonNullElse(pastParticipants, List.of());
    }

    public Jid groupJid() {
        return groupJid;
    }

    public List<ChatPastParticipant> pastParticipants() {
        return pastParticipants;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GroupPastParticipants that
                && Objects.equals(groupJid, that.groupJid)
                && Objects.equals(pastParticipants, that.pastParticipants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupJid, pastParticipants);
    }

    @Override
    public String toString() {
        return "GroupPastParticipants[" +
                "groupJid=" + groupJid + ", " +
                "pastParticipants=" + pastParticipants + ']';
    }
}