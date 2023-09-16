package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a list of past participants in a chat group
 */
public record PastParticipants(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        ContactJid groupJid,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT, repeated = true)
        @NonNull
        List<PastParticipant> pastParticipants
) implements ProtobufMessage {

}