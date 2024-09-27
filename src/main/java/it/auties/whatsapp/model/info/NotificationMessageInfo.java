package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

@ProtobufMessage(name = "NotificationMessageInfo")
public record NotificationMessageInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ChatMessageKey key,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        MessageContainer message,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long messageTimestampSeconds,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> participant
) implements Info {
    /**
     * Returns when the message was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> messageTimestamp() {
        return Clock.parseSeconds(messageTimestampSeconds);
    }
}
