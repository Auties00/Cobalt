package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.poll.PollEncValue;
import it.auties.whatsapp.model.poll.PollUpdateMessageMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollUpdateMessage")
public final class PollUpdateMessage implements Message {
    private static final String POLL_NAME = "Poll Vote";

    @ProtobufProperty(index = 1, name = "pollCreationMessageKey", type = ProtobufType.MESSAGE)
    private MessageKey pollCreationMessageKey;

    @ProtobufProperty(index = 2, name = "vote", type = ProtobufType.MESSAGE)
    private PollEncValue vote;

    @ProtobufProperty(index = 3, name = "metadata", type = ProtobufType.MESSAGE)
    private PollUpdateMessageMetadata metadata;

    @ProtobufProperty(index = 4, name = "senderTimestampMs", type = ProtobufType.INT64)
    private long senderTimestampMilliseconds;

    public String secretName(){
        return POLL_NAME;
    }

    @Override
    public MessageType type() {
        return MessageType.POLL_UPDATE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
