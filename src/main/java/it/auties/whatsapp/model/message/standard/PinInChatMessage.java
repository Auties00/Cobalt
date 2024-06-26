package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.*;


@ProtobufMessageName("Message.PinInChatMessage")
public record PinInChatMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        ChatMessageKey key,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Type pinType,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long senderTimestampMilliseconds
) implements Message {
    @Override
    public MessageType type() { return MessageType.PIN_IN_CHAT; }

    @Override
    public MessageCategory category() { return MessageCategory.STANDARD; }

    @ProtobufMessageName("Message.PinInChatMessage.Type")
    public enum Type implements ProtobufEnum {
        UNKNOWN_TYPE(0),
        PIN_FOR_ALL(1),
        UNPIN_FOR_ALL(2);

        final int index;

        Type(int index) { this.index = index; }

        public int index() { return index; }
    }
}
