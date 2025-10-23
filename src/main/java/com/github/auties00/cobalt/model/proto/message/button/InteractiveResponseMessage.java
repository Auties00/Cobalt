package com.github.auties00.cobalt.model.proto.message.button;

import com.github.auties00.cobalt.model.proto.button.interactive.InteractiveBody;
import com.github.auties00.cobalt.model.proto.info.ContextInfo;
import com.github.auties00.cobalt.model.proto.message.model.ContextualMessage;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "Message.InteractiveResponseMessage")
public final class InteractiveResponseMessage implements ContextualMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final InteractiveBody body;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final NativeFlowResponseMessage nativeFlowResponseMessage;

    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    InteractiveResponseMessage(InteractiveBody body, NativeFlowResponseMessage nativeFlowResponseMessage, ContextInfo contextInfo) {
        this.body = body;
        this.nativeFlowResponseMessage = nativeFlowResponseMessage;
        this.contextInfo = contextInfo;
    }

    @Override
    public Type type() {
        return Type.INTERACTIVE_RESPONSE;
    }

    @Override
    public Category category() {
        return Category.BUTTON;
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
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    @Override
    public String toString() {
        return "InteractiveResponseMessage[" +
                "body=" + body + ", " +
                "nativeFlowResponseMessage=" + nativeFlowResponseMessage + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}