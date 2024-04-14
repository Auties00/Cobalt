package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

@ProtobufMessageName("NotificationMessageInfo")
public record NotificationMessageInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        ChatMessageKey key,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        MessageContainer message,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long messageTimestampSeconds,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> participant
) implements Info, ProtobufMessage {
    /**
     * Returns when the message was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> messageTimestamp() {
        return Clock.parseSeconds(messageTimestampSeconds);
    }
}
