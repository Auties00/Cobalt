package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

import java.util.List;

/**
 * Class representing a list of past participants in a chat group
 */
@ProtobufMessage(name = "PastParticipants")
public record GroupPastParticipants(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid groupJid,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        List<ChatPastParticipant> pastParticipants
) {

}