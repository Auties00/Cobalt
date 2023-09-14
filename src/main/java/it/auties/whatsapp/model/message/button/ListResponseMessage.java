package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.misc.SingleSelectReplyButton;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a message that contains a response to a previous
 * {@link ListMessage}
 */
public record ListResponseMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String title,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        @NonNull
        SingleSelectReplyButton reply,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> description,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        ListMessageType listType
) implements ButtonReplyMessage {
    @Override
    public MessageType type() {
        return MessageType.LIST_RESPONSE;
    }
}