package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.Message;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message that contains a highly structured message inside. Not
 * really clear how this could be used, contributions are welcomed.
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage")
public final class HighlyStructuredMessage implements ButtonMessage, HighlyStructuredFourRowTemplateTitle {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String namespace;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String elementName;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> params;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String fallbackLg;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String fallbackLc;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final List<HighlyStructuredLocalizableParameter> localizableParameters;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String deterministicLg;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String deterministicLc;

    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final TemplateMessage templateMessage;

    HighlyStructuredMessage(String namespace, String elementName, List<String> params, String fallbackLg, String fallbackLc, List<HighlyStructuredLocalizableParameter> localizableParameters, String deterministicLg, String deterministicLc, TemplateMessage templateMessage) {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.elementName = Objects.requireNonNull(elementName, "elementName cannot be null");
        this.params = Objects.requireNonNullElse(params, List.of());
        this.fallbackLg = fallbackLg;
        this.fallbackLc = fallbackLc;
        this.localizableParameters = Objects.requireNonNullElse(localizableParameters, List.of());
        this.deterministicLg = deterministicLg;
        this.deterministicLc = deterministicLc;
        this.templateMessage = Objects.requireNonNull(templateMessage, "templateMessage cannot be null");
    }

    public String namespace() {
        return namespace;
    }

    public String elementName() {
        return elementName;
    }

    public List<String> params() {
        return params;
    }

    public Optional<String> fallbackLg() {
        return Optional.ofNullable(fallbackLg);
    }

    public Optional<String> fallbackLc() {
        return Optional.ofNullable(fallbackLc);
    }

    public List<HighlyStructuredLocalizableParameter> localizableParameters() {
        return localizableParameters;
    }

    public Optional<String> deterministicLg() {
        return Optional.ofNullable(deterministicLg);
    }

    public Optional<String> deterministicLc() {
        return Optional.ofNullable(deterministicLc);
    }

    public TemplateMessage templateMessage() {
        return templateMessage;
    }

    @Override
    public Message.Type type() {
        return Message.Type.HIGHLY_STRUCTURED;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return HighlyStructuredFourRowTemplateTitle.Type.HIGHLY_STRUCTURED;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredMessage that
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(elementName, that.elementName)
                && Objects.equals(params, that.params)
                && Objects.equals(fallbackLg, that.fallbackLg)
                && Objects.equals(fallbackLc, that.fallbackLc)
                && Objects.equals(localizableParameters, that.localizableParameters)
                && Objects.equals(deterministicLg, that.deterministicLg)
                && Objects.equals(deterministicLc, that.deterministicLc)
                && Objects.equals(templateMessage, that.templateMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, elementName, params, fallbackLg, fallbackLc, localizableParameters, deterministicLg, deterministicLc, templateMessage);
    }

    @Override
    public String toString() {
        return "HighlyStructuredMessage[" +
                "namespace=" + namespace + ", " +
                "elementName=" + elementName + ", " +
                "params=" + params + ", " +
                "fallbackLg=" + fallbackLg + ", " +
                "fallbackLc=" + fallbackLc + ", " +
                "localizableParameters=" + localizableParameters + ", " +
                "deterministicLg=" + deterministicLg + ", " +
                "deterministicLc=" + deterministicLc + ", " +
                "templateMessage=" + templateMessage + ']';
    }
}