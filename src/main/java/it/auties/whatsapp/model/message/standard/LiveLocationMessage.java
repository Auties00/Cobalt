package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;


/**
 * A model class that represents a message holding a live location inside
 */
public record LiveLocationMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
        double latitude,
        @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
        double longitude,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        int accuracy,
        @ProtobufProperty(index = 4, type = ProtobufType.FLOAT)
        float speed,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
        int magneticNorthOffset,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> caption,
        @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
        long sequenceNumber,
        @ProtobufProperty(index = 8, type = ProtobufType.UINT32)
        int timeOffset,
        @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage {
    @Override
    public MessageType type() {
        return MessageType.LIVE_LOCATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}