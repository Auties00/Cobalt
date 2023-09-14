package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.TemplateFormatter;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.BytesHelper;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message sent in a WhatsappBusiness chat that provides a list of
 * buttons to choose from.
 */
public record TemplateMessage(
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        @NonNull
        String id,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        HydratedFourRowTemplate content,
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredFourRowTemplate> highlyStructuredFourRowTemplateFormat,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<HydratedFourRowTemplate> hydratedFourRowTemplateFormat,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<InteractiveMessage> interactiveMessageFormat,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage, ButtonMessage {
    @ProtobufBuilder(className = "TemplateMessageSimpleBuilder")
    static TemplateMessage customBuilder(@Nullable String id, @NonNull HydratedFourRowTemplate content, @Nullable TemplateFormatter format, @Nullable ContextInfo contextInfo) {
        var builder = new TemplateMessageBuilder()
                .id(Objects.requireNonNullElseGet(id, () -> HexFormat.of().formatHex(BytesHelper.random(6))))
                .content(content)
                .contextInfo(contextInfo);
        switch (format){
            case HighlyStructuredFourRowTemplate highlyStructuredFourRowTemplate -> builder.highlyStructuredFourRowTemplateFormat(highlyStructuredFourRowTemplate);
            case HydratedFourRowTemplate hydratedFourRowTemplate -> builder.hydratedFourRowTemplateFormat(hydratedFourRowTemplate);
            case InteractiveMessage interactiveMessage -> builder.interactiveMessageFormat(interactiveMessage);
            case null -> {}
        }
        return builder.build();
    }

    /**
     * Returns the type of format of this message
     *
     * @return a non-null {@link TemplateFormatterType}
     */
    public TemplateFormatterType formatType() {
        return format().map(TemplateFormatter::templateType)
                .orElse(TemplateFormatterType.NONE);
    }

    /**
     * Returns the formatter of this message
     *
     * @return an optional
     */
    public Optional<? extends TemplateFormatter> format() {
        if (highlyStructuredFourRowTemplateFormat.isPresent()) {
            return highlyStructuredFourRowTemplateFormat;
        }

        if (hydratedFourRowTemplateFormat.isPresent()) {
            return hydratedFourRowTemplateFormat;
        }

        return interactiveMessageFormat;
    }

    @Override
    public MessageType type() {
        return MessageType.TEMPLATE;
    }
}
