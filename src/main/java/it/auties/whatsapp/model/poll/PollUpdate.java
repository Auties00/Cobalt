package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Not currently used, so it's
 * package private
 */
@ProtobufMessageName("PollUpdate")
public record PollUpdate(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        ChatMessageKey pollUpdateMessageKey,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        PollUpdateEncryptedOptions vote,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long senderTimestampMilliseconds
) implements ProtobufMessage {
    /**
     * Returns when the update was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> senderTimestamp() {
        return Clock.parseMilliseconds(senderTimestampMilliseconds);
    }
}