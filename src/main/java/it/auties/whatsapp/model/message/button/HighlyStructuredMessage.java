package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.business.BusinessLocalizableParameter;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplateTitle;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplateTitleType;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message that contains a highly structured message inside. Not
 * really clear how this could be used, contributions are welcomed.
 */
public record HighlyStructuredMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String namespace,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String elementName,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING, repeated = true)
        @NonNull
        List<String> params,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> fallbackLg,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> fallbackLc,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT, repeated = true)
        @NonNull
        List<BusinessLocalizableParameter> localizableParameters,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> deterministicLg,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        Optional<String> deterministicLc,
        @ProtobufProperty(index = 9, type = ProtobufType.OBJECT)
        @NonNull
        TemplateMessage templateMessage
) implements ButtonMessage, HighlyStructuredFourRowTemplateTitle {
    @Override
    public MessageType type() {
        return MessageType.HIGHLY_STRUCTURED;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitleType titleType() {
        return HighlyStructuredFourRowTemplateTitleType.HIGHLY_STRUCTURED;
    }
}
