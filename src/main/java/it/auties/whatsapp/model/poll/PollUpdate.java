package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Not currently used, so it's
 * package private
 */
public record PollUpdate(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        MessageKey pollUpdateMessageKey,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
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