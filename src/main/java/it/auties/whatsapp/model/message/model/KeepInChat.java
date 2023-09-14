package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.standard.KeepInChatType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that represents an ephemeral message that was saved manually by the user in a chat
 */
public record KeepInChat(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        KeepInChatType keepType,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long serverTimestampSeconds,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        MessageKey key,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        ContactJid deviceJid,
        @ProtobufProperty(index = 5, type = ProtobufType.INT64)
        long clientTimestampInMilliseconds,
        @ProtobufProperty(index = 6, type = ProtobufType.INT64)
        long serverTimestampMilliseconds
) implements ProtobufMessage {
    public Optional<ZonedDateTime> serverTimestamp(){
        return Clock.parseSeconds(serverTimestampSeconds);
    }

    public Optional<ZonedDateTime> clientTimestamp(){
        return Clock.parseMilliseconds(clientTimestampInMilliseconds);
    }
}