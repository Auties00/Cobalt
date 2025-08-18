package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that represents an ephemeral message that was saved manually by the user in a chat
 */
@ProtobufMessage(name = "KeepInChat")
public record KeepInChat(
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        KeepInChatType keepType,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long serverTimestampSeconds,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        ChatMessageKey key,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Jid deviceJid,
        @ProtobufProperty(index = 5, type = ProtobufType.INT64)
        long clientTimestampInMilliseconds,
        @ProtobufProperty(index = 6, type = ProtobufType.INT64)
        long serverTimestampMilliseconds
) {
    public Optional<ZonedDateTime> serverTimestamp() {
        return Clock.parseSeconds(serverTimestampSeconds);
    }

    public Optional<ZonedDateTime> clientTimestamp() {
        return Clock.parseMilliseconds(clientTimestampInMilliseconds);
    }
}