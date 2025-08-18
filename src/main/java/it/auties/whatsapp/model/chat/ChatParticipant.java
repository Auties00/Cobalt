package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.whatsapp.model.jid.Jid;

public abstract sealed class ChatParticipant permits GroupParticipant, CommunityParticipant {
    public abstract Jid jid();

    @ProtobufDeserializer
    public static ChatParticipant of(byte[] data) {
        try {
            return CommunityParticipantSpec.decode(data);
        }catch (ProtobufDeserializationException exception) {
            return GroupParticipantSpec.decode(data);
        }
    }

    @ProtobufSerializer
    public byte[] toBytes() {
        return switch (this) {
            case CommunityParticipant communityParticipant -> CommunityParticipantSpec.encode(communityParticipant);
            case GroupParticipant groupParticipant -> GroupParticipantSpec.encode(groupParticipant);
        };
    }
}
