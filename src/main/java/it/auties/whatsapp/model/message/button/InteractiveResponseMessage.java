package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.button.interactive.InteractiveBody;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

@ProtobufMessageName("Message.InteractiveResponseMessage")
public record InteractiveResponseMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        InteractiveBody body,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        NativeFlowResponseMessage nativeFlowResponseMessage,
        @ProtobufProperty(index = 15, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage {
    @Override
    public MessageType type() {
        return MessageType.INTERACTIVE_RESPONSE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.BUTTON;
    }

    public InteractiveMessageContent.Type interactiveResponseMessageType() {
        return InteractiveMessageContent.Type.COLLECTION;
    }
}