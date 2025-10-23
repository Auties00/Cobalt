package com.github.auties00.cobalt.model.proto.message.button;

import com.github.auties00.cobalt.model.proto.button.template.highlyStructured.HighlyStructuredMessage;
import com.github.auties00.cobalt.model.proto.info.ContextInfo;
import com.github.auties00.cobalt.model.proto.message.model.ButtonReplyMessage;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;


/**
 * A model class that represents a message that contains a newsletters to a previous
 * {@link HighlyStructuredMessage}
 */
@ProtobufMessage(name = "Message.TemplateButtonReplyMessage")
public final class TemplateReplyMessage implements ButtonReplyMessage<TemplateReplyMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String buttonText;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    final int index;

    TemplateReplyMessage(String id, String buttonText, ContextInfo contextInfo, int index) {
        this.id = id;
        this.buttonText = buttonText;
        this.contextInfo = contextInfo;
        this.index = index;
    }

    @Override
    public Type type() {
        return Type.TEMPLATE_REPLY;
    }

    public String id() {
        return id;
    }

    public String buttonText() {
        return buttonText;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        return "TemplateReplyMessage[" +
                "id=" + id + ", " +
                "buttonText=" + buttonText + ", " +
                "contextInfo=" + contextInfo + ", " +
                "index=" + index + ']';
    }
}