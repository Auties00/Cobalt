package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message that contains a highly structured message inside. Not
 * really clear how this could be used, contributions are welcomed.
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage")
public record HighlyStructuredMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String namespace,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String elementName,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        List<String> params,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> fallbackLg,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> fallbackLc,
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        List<HighlyStructuredLocalizableParameter> localizableParameters,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> deterministicLg,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        Optional<String> deterministicLc,
        @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
        TemplateMessage templateMessage
) implements ButtonMessage, HighlyStructuredFourRowTemplateTitle {
    @Override
    public MessageType type() {
        return MessageType.HIGHLY_STRUCTURED;
    }

    @Override
    public Type titleType() {
        return Type.HIGHLY_STRUCTURED;
    }
}
