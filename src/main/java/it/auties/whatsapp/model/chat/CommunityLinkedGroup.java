package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.OptionalInt;

/**
 * A model class that represents a group linked to a community
 */
@ProtobufMessage(name = "CommunityLinkedGroup")
public final class CommunityLinkedGroup {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    private final Integer participants;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CommunityLinkedGroup(Jid jid, Integer participants) {
        this.jid = jid;
        this.participants = participants;
    }

    public Jid jid() {
        return jid;
    }

    public OptionalInt participants() {
        return OptionalInt.of(participants);
    }
}
