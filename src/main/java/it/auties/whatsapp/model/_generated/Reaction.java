package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("Reaction")
public class Reaction implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "key", type = MESSAGE)
    private MessageKey key;

    @ProtobufProperty(index = 2, name = "text", type = STRING)
    private String text;

    @ProtobufProperty(index = 3, name = "groupingKey", type = STRING)
    private String groupingKey;

    @ProtobufProperty(index = 4, name = "senderTimestampMs", type = INT64)
    private Long senderTimestampMs;

    @ProtobufProperty(index = 5, name = "unread", type = BOOL)
    private Boolean unread;
}