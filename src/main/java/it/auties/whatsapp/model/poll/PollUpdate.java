package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
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
@ProtobufMessage(name = "PollUpdate")
public record PollUpdate(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ChatMessageKey pollUpdateMessageKey,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        PollUpdateEncryptedOptions vote,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long senderTimestampMilliseconds
) {
    /**
     * Returns when the update was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> senderTimestamp() {
        return Clock.parseMilliseconds(senderTimestampMilliseconds);
    }
}