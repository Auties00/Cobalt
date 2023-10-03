package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * Class representing a list of past participants in a chat group
 */
@ProtobufMessageName("PastParticipants")
public record GroupPastParticipants(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        Jid groupJid,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT, repeated = true)
        @NonNull
        List<GroupPastParticipant> pastParticipants
) implements ProtobufMessage {

}