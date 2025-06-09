package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Objects;

@ProtobufMessage
public final class ChatParticipant {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    ChatRole role;

    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    Type type;

    ChatParticipant(Jid jid, ChatRole role, Type type) {
        this.jid = jid;
        this.role = role;
        this.type = type;
    }

    public static ChatParticipant ofGroup(Jid jid, ChatRole role) {
        return new ChatParticipant(jid, role, Type.GROUP);
    }

    public static ChatParticipant ofCommunity(Jid jid) {
        return new ChatParticipant(jid, ChatRole.USER, Type.COMMUNITY);
    }

    public Jid jid() {
        return jid;
    }

    public ChatRole role() {
        return role;
    }

    public void setRole(ChatRole role) {
        switch (type) {
            case GROUP -> this.role = role;
            case COMMUNITY -> {}
        }
    }

    @Override
    public String toString() {
        return "ChatParticipant[" +
                "jid=" + jid +
                ", role=" + role +
                ']';
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, role, type);
    }

    @ProtobufEnum
    public enum Type {
        GROUP,
        COMMUNITY
    }
}
