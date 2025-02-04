package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Objects;

/**
 * A model class that represents the participant of a community
 */
@ProtobufMessage
public final class CommunityParticipant extends ChatParticipant {
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final Jid jid;
    public CommunityParticipant(Jid jid) {
        this.jid = jid;
    }

    @Override
    public Jid jid() {
        return jid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CommunityParticipant) obj;
        return Objects.equals(this.jid, that.jid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid);
    }

    @Override
    public String toString() {
        return "CommunityParticipant[" +
                "jid=" + jid + ']';
    }
}
