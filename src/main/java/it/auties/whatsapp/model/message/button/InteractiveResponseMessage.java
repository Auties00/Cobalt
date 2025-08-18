package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.interactive.InteractiveBody;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;

@ProtobufMessage(name = "Message.InteractiveResponseMessage")
public final class InteractiveResponseMessage implements ContextualMessage<InteractiveResponseMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final InteractiveBody body;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final NativeFlowResponseMessage nativeFlowResponseMessage;
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public InteractiveResponseMessage(InteractiveBody body, NativeFlowResponseMessage nativeFlowResponseMessage, ContextInfo contextInfo) {
        this.body = body;
        this.nativeFlowResponseMessage = nativeFlowResponseMessage;
        this.contextInfo = contextInfo;
    }

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

    public InteractiveBody body() {
        return body;
    }

    public NativeFlowResponseMessage nativeFlowResponseMessage() {
        return nativeFlowResponseMessage;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public InteractiveResponseMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "InteractiveResponseMessage[" +
                "body=" + body + ", " +
                "nativeFlowResponseMessage=" + nativeFlowResponseMessage + ", " +
                "contextInfo=" + contextInfo + ']';
    }

}