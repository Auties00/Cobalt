package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * A message that contains information related to a call
 */
@ProtobufMessageName("Message.Call")
public record CallMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte @NonNull [] key,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String source,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte @NonNull [] data,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int delay
) implements Message {
    @Override
    public MessageType type() {
        return MessageType.CALL;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}