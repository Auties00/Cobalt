package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model class that represents metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Not currently used, so it's
 * package private
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollUpdate")
public class PollUpdate implements ProtobufMessage {
    /**
     * The message key
     */
    @ProtobufProperty(index = 1, name = "pollUpdateMessageKey", type = MESSAGE)
    private MessageKey pollUpdateMessageKey;

    /**
     * The vote
     */
    @ProtobufProperty(index = 2, name = "vote", type = MESSAGE)
    private PollUpdateEncryptedOptions vote;

    /**
     * The timestamp
     */
    @ProtobufProperty(index = 3, name = "senderTimestampMs", type = INT64)
    private long senderTimestampMilliseconds;
}