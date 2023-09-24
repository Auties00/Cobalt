package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;


/**
 * A model class that represents a message that contains a response to a previous
 * {@link HighlyStructuredMessage}
 */
@ProtobufMessageName("Message.TemplateButtonReplyMessage")
public record TemplateReplyMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String buttonText,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int index
) implements ButtonReplyMessage {
    @Override
    public MessageType type() {
        return MessageType.TEMPLATE_REPLY;
    }
}