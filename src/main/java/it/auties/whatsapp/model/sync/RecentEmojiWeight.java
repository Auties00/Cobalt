package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.FLOAT;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("RecentEmojiWeight")
public record RecentEmojiWeight(@ProtobufProperty(index = 1, type = STRING) String emoji,
                                @ProtobufProperty(index = 2, type = FLOAT) Float weight) implements ProtobufMessage {
}
