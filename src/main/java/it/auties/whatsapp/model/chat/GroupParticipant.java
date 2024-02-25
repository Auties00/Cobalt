package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Objects;

/**
 * A model class that represents a participant of a group.
 */
@ProtobufMessageName("GroupParticipant")
public final class GroupParticipant implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
    private GroupRole role;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GroupParticipant(Jid jid, GroupRole role) {
        this.jid = jid;
        this.role = Objects.requireNonNullElse(role, GroupRole.USER);
    }

    public Jid jid() {
        return jid;
    }

    public GroupRole role() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "GroupParticipant{" +
                "jid=" + jid +
                ", role=" + role +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, role.index());
    }
}
