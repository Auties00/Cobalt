package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.TemplateFormatter;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Bytes;

import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message sent in a WhatsappBusiness chat that provides a list of
 * buttons to choose from.
 */
@ProtobufMessage(name = "Message.TemplateMessage")
public final class TemplateMessage implements ContextualMessage<TemplateMessage>, ButtonMessage {
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    private final String id;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final HydratedFourRowTemplate content;
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final HighlyStructuredFourRowTemplate highlyStructuredFourRowTemplateFormat;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final HydratedFourRowTemplate hydratedFourRowTemplateFormat;
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final InteractiveMessage interactiveMessageFormat;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public TemplateMessage(String id, HydratedFourRowTemplate content, HighlyStructuredFourRowTemplate highlyStructuredFourRowTemplateFormat, HydratedFourRowTemplate hydratedFourRowTemplateFormat, InteractiveMessage interactiveMessageFormat, ContextInfo contextInfo) {
        this.id = id;
        this.content = content;
        this.highlyStructuredFourRowTemplateFormat = highlyStructuredFourRowTemplateFormat;
        this.hydratedFourRowTemplateFormat = hydratedFourRowTemplateFormat;
        this.interactiveMessageFormat = interactiveMessageFormat;
        this.contextInfo = contextInfo;
    }

    @ProtobufBuilder(className = "TemplateMessageSimpleBuilder")
    static TemplateMessage customBuilder(String id, HydratedFourRowTemplate content, TemplateFormatter format, ContextInfo contextInfo) {
        var builder = new TemplateMessageBuilder()
                .id(Objects.requireNonNullElseGet(id, () -> HexFormat.of().formatHex(Bytes.random(6))))
                .content(content)
                .contextInfo(contextInfo);
        switch (format) {
            case HighlyStructuredFourRowTemplate highlyStructuredFourRowTemplate ->
                    builder.highlyStructuredFourRowTemplateFormat(highlyStructuredFourRowTemplate);
            case HydratedFourRowTemplate hydratedFourRowTemplate ->
                    builder.hydratedFourRowTemplateFormat(hydratedFourRowTemplate);
            case InteractiveMessage interactiveMessage -> builder.interactiveMessageFormat(interactiveMessage);
            case null -> {
            }
        }
        return builder.build();
    }

    /**
     * Returns the type of format of this message
     *
     * @return a non-null {@link TemplateFormatter.Type}
     */
    public TemplateFormatter.Type formatType() {
        return format().map(TemplateFormatter::templateType)
                .orElse(TemplateFormatter.Type.NONE);
    }

    /**
     * Returns the formatter of this message
     *
     * @return an optional
     */
    public Optional<? extends TemplateFormatter> format() {
        if (highlyStructuredFourRowTemplateFormat != null) {
            return Optional.of(highlyStructuredFourRowTemplateFormat);
        }

        if (hydratedFourRowTemplateFormat != null) {
            return Optional.of(hydratedFourRowTemplateFormat);
        }

        return Optional.ofNullable(interactiveMessageFormat);
    }

    @Override
    public MessageType type() {
        return MessageType.TEMPLATE;
    }

    public String id() {
        return id;
    }

    public HydratedFourRowTemplate content() {
        return content;
    }

    public Optional<HighlyStructuredFourRowTemplate> highlyStructuredFourRowTemplateFormat() {
        return Optional.ofNullable(highlyStructuredFourRowTemplateFormat);
    }

    public Optional<HydratedFourRowTemplate> hydratedFourRowTemplateFormat() {
        return Optional.ofNullable(hydratedFourRowTemplateFormat);
    }

    public Optional<InteractiveMessage> interactiveMessageFormat() {
        return Optional.ofNullable(interactiveMessageFormat);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public TemplateMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "TemplateMessage[" +
                "id=" + id + ", " +
                "content=" + content + ", " +
                "highlyStructuredFourRowTemplateFormat=" + highlyStructuredFourRowTemplateFormat + ", " +
                "hydratedFourRowTemplateFormat=" + hydratedFourRowTemplateFormat + ", " +
                "interactiveMessageFormat=" + interactiveMessageFormat + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}
