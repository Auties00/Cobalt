package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;


/**
 * A model class that represents a message that contains a newsletters to a previous
 * {@link HighlyStructuredMessage}
 */
@ProtobufMessage(name = "Message.TemplateButtonReplyMessage")
public final class TemplateReplyMessage implements ButtonReplyMessage<TemplateReplyMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String id;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String buttonText;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    private final int index;

    public TemplateReplyMessage(String id, String buttonText, ContextInfo contextInfo, int index) {
        this.id = id;
        this.buttonText = buttonText;
        this.contextInfo = contextInfo;
        this.index = index;
    }

    @Override
    public MessageType type() {
        return MessageType.TEMPLATE_REPLY;
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
    public TemplateReplyMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
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