package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollUpdate")
public class PollUpdate
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "pollUpdateMessageKey", type = ProtobufType.MESSAGE)
    private MessageKey pollUpdateMessageKey;

    @ProtobufProperty(index = 2, name = "vote", type = ProtobufType.MESSAGE)
    private PollVoteMessage vote;

    @ProtobufProperty(index = 3, name = "senderTimestampMs", type = ProtobufType.INT64)
    private long senderTimestampMilliseconds;
}