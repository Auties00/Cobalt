package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.MessageAddOnContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that represents an ephemeral message that was saved manually by the user in a chat
 */
@ProtobufMessageName("PinInChat")
public record PinInChat(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Type pinType,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        ChatMessageKey key,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long clientTimestampInMilliseconds,
        @ProtobufProperty(index = 4, type = ProtobufType.INT64)
        long serverTimestampMilliseconds,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        MessageAddOnContextInfo messageAddOnContextInfo
) implements ProtobufMessage {
    public Optional<ZonedDateTime> serverTimestamp() { return Clock.parseMilliseconds(serverTimestampMilliseconds); }

    public Optional<ZonedDateTime> clientTimestamp() { return Clock.parseMilliseconds(clientTimestampInMilliseconds); }

    public enum Type implements ProtobufEnum {
        UNKNOWN_TYPE(0),
        PIN_FOR_ALL(1),
        UNDO_PIN_FOR_ALL(2);

        final int index;

        Type(@ProtobufEnumIndex int index) { this.index = index; }

        public int index() { return index; }
    }
}