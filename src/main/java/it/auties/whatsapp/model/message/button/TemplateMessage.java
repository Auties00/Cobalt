package it.auties.whatsapp.model.message.button;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.button.FourRowTemplate;
import it.auties.whatsapp.model.button.HydratedFourRowTemplate;
import it.auties.whatsapp.model.button.TemplateFormatter;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that represents a message sent in a WhatsappBusiness chat that provides a list of
 * buttons to choose from.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("TemplateMessage")
public final class TemplateMessage extends ContextualMessage implements ButtonMessage {
    /**
     * The id of this template
     */
    @ProtobufProperty(index = 9, type = STRING)
    private String id;

    /**
     * Hydrated template
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = HydratedFourRowTemplate.class)
    private HydratedFourRowTemplate content;

    /**
     * Four row template. This property is defined only if {@link TemplateMessage#formatType()} ==
     * {@link TemplateFormatterType#FOUR_ROW}.
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = FourRowTemplate.class)
    private FourRowTemplate fourRowTemplateFormat;

    /**
     * Hydrated four row template. This property is defined only if
     * {@link TemplateMessage#formatType()} == {@link TemplateFormatterType#HYDRATED_FOUR_ROW}.
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HydratedFourRowTemplate.class)
    private HydratedFourRowTemplate hydratedFourRowTemplateFormat;

    /**
     * Interactive message. This property is defined only if {@link TemplateMessage#formatType()} ==
     * {@link TemplateFormatterType#INTERACTIVE}.
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = InteractiveMessage.class)
    private InteractiveMessage interactiveMessageFormat;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ContextInfo.class)
    @Default
    private ContextInfo contextInfo = new ContextInfo();

    /**
     * Constructs a new template message
     *
     * @param template the non-null template
     * @return a non-null template message
     */
    public static TemplateMessage of(@NonNull HydratedFourRowTemplate template) {
        return of(template, (ContextInfo) null);
    }

    /**
     * Constructs a new template message
     *
     * @param template the non-null template
     * @return a non-null template message
     */
    public static TemplateMessage of(@NonNull HydratedFourRowTemplate template, ContextInfo contextInfo) {
        return of(template, FourRowTemplate.of(), contextInfo);
    }

    /**
     * Constructs a new template message
     *
     * @param content     the non-null template
     * @param contextInfo the nullable context info
     * @return a non-null template message
     */
    public static TemplateMessage of(@NonNull HydratedFourRowTemplate content, @NonNull TemplateFormatter formatter, ContextInfo contextInfo) {
        var builder = TemplateMessage.builder()
                .id(Bytes.ofRandom(6).toHex())
                .content(content)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new));
        switch (formatter) {
            case FourRowTemplate fourRowTemplate -> builder.fourRowTemplateFormat(fourRowTemplate);
            case HydratedFourRowTemplate hydratedFourRowTemplate ->
                    builder.hydratedFourRowTemplateFormat(hydratedFourRowTemplate);
            case InteractiveMessage interactiveMessage -> builder.interactiveMessageFormat(interactiveMessage);
        }
        return builder.build();
    }

    /**
     * Constructs a new template message
     *
     * @param template the non-null template
     * @return a non-null template message
     */
    public static TemplateMessage of(@NonNull HydratedFourRowTemplate template, @NonNull TemplateFormatter formatter) {
        return of(template, formatter, null);
    }

    /**
     * Returns the type of format of this message
     *
     * @return a non-null {@link TemplateFormatterType}
     */
    public TemplateFormatterType formatType() {
        return format().map(TemplateFormatter::templateType).orElse(TemplateFormatterType.NONE);
    }

    /**
     * Returns the formatter of this message
     *
     * @return an optional
     */
    public Optional<TemplateFormatter> format() {
        if (fourRowTemplateFormat != null) {
            return Optional.of(fourRowTemplateFormat);
        }
        if (hydratedFourRowTemplateFormat != null) {
            return Optional.of(hydratedFourRowTemplateFormat);
        }
        if (interactiveMessageFormat != null) {
            return Optional.of(interactiveMessageFormat);
        }
        return Optional.empty();
    }

    /**
     * Returns the four rows formatter of this message if present
     *
     * @return an optional
     */
    public Optional<FourRowTemplate> fourRowTemplateFormat() {
        return Optional.ofNullable(fourRowTemplateFormat);
    }

    /**
     * Returns the hydrated four rows formatter of this message if present
     *
     * @return an optional
     */
    public Optional<HydratedFourRowTemplate> hydratedFourRowTemplateFormat() {
        return Optional.ofNullable(hydratedFourRowTemplateFormat);
    }

    /**
     * Returns the interactive formatter of this message if present
     *
     * @return an optional
     */
    public Optional<InteractiveMessage> interactiveMessageFormat() {
        return Optional.ofNullable(interactiveMessageFormat);
    }

    @Override
    public MessageType type() {
        return MessageType.TEMPLATE;
    }
}
